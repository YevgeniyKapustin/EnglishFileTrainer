package main

import (
	"bytes"
	"crypto/subtle"
	"encoding/json"
	"errors"
	"io"
	"log/slog"
	"net/http"
	"strings"
	"time"
)

const (
	upstreamModel        = "deepseek-chat"
	maxUpstreamBodyBytes = 4 << 20
)

type chatRequest struct {
	Messages    json.RawMessage `json:"messages"`
	Temperature *float64        `json:"temperature,omitempty"`
	MaxTokens   *int            `json:"max_tokens,omitempty"`
}

type upstreamRequest struct {
	Model       string          `json:"model"`
	Messages    json.RawMessage `json:"messages"`
	Temperature *float64        `json:"temperature,omitempty"`
	MaxTokens   int             `json:"max_tokens"`
}

type upstreamUsage struct {
	Usage struct {
		TotalTokens int64 `json:"total_tokens"`
	} `json:"usage"`
}

type proxy struct {
	cfg      config
	client   *http.Client
	limiters *keyLimiters
	budget   *dailyBudget
	logger   *slog.Logger
}

func newProxy(cfg config, client *http.Client, logger *slog.Logger, now func() time.Time) *proxy {
	return &proxy{
		cfg:      cfg,
		client:   client,
		limiters: newKeyLimiters(cfg.ratePerMin),
		budget:   newDailyBudget(cfg.dailyTokenLimit, now),
		logger:   logger,
	}
}

func (p *proxy) routes() http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("/v1/chat", p.handleChat)
	mux.HandleFunc("/healthz", handleHealth)
	return mux
}

func handleHealth(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		w.Header().Set("Allow", http.MethodGet)
		writeError(w, http.StatusMethodNotAllowed, "method_not_allowed")
		return
	}
	w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	_, _ = io.WriteString(w, "ok\n")
}

func (p *proxy) handleChat(w http.ResponseWriter, r *http.Request) {
	started := time.Now()
	status, tokens := p.serveChat(w, r)
	p.logger.Info("chat",
		"status", status,
		"latency_ms", time.Since(started).Milliseconds(),
		"tokens", tokens,
	)
}

func (p *proxy) serveChat(w http.ResponseWriter, r *http.Request) (int, int64) {
	if r.Method != http.MethodPost {
		w.Header().Set("Allow", http.MethodPost)
		return writeError(w, http.StatusMethodNotAllowed, "method_not_allowed"), 0
	}
	appKeyID, ok := p.authenticate(r.Header.Get("Authorization"))
	if !ok {
		return writeError(w, http.StatusUnauthorized, "unauthorized"), 0
	}
	if !p.limiters.allow(appKeyID) {
		return writeError(w, http.StatusTooManyRequests, "rate_limited"), 0
	}
	if p.budget.exhausted() {
		return writeError(w, http.StatusTooManyRequests, "daily_limit"), 0
	}
	payload, status := p.upstreamPayload(w, r)
	switch status {
	case http.StatusOK:
		return p.forward(w, r, payload)
	case http.StatusRequestEntityTooLarge:
		return writeError(w, status, "body_too_large"), 0
	default:
		return writeError(w, status, "bad_request"), 0
	}
}

func (p *proxy) authenticate(header string) (int, bool) {
	token, found := strings.CutPrefix(header, "Bearer ")
	if !found || token == "" {
		return 0, false
	}
	matchedID := -1
	for id, key := range p.cfg.appKeys {
		if subtle.ConstantTimeCompare([]byte(token), []byte(key)) == 1 {
			matchedID = id
		}
	}
	return matchedID, matchedID >= 0
}

func (p *proxy) upstreamPayload(w http.ResponseWriter, r *http.Request) ([]byte, int) {
	body, err := io.ReadAll(http.MaxBytesReader(w, r.Body, p.cfg.maxBodyBytes))
	var tooLarge *http.MaxBytesError
	switch {
	case errors.As(err, &tooLarge):
		return nil, http.StatusRequestEntityTooLarge
	case err != nil:
		return nil, http.StatusBadRequest
	}
	var incoming chatRequest
	if err := json.Unmarshal(body, &incoming); err != nil || len(incoming.Messages) == 0 {
		return nil, http.StatusBadRequest
	}
	payload, err := json.Marshal(upstreamRequest{
		Model:       upstreamModel,
		Messages:    incoming.Messages,
		Temperature: incoming.Temperature,
		MaxTokens:   p.cappedMaxTokens(incoming.MaxTokens),
	})
	if err != nil {
		return nil, http.StatusBadRequest
	}
	return payload, http.StatusOK
}

func (p *proxy) cappedMaxTokens(requested *int) int {
	if requested == nil || *requested <= 0 || *requested > p.cfg.maxTokens {
		return p.cfg.maxTokens
	}
	return *requested
}

func (p *proxy) forward(w http.ResponseWriter, r *http.Request, payload []byte) (int, int64) {
	request, err := http.NewRequestWithContext(r.Context(), http.MethodPost, p.cfg.upstreamURL, bytes.NewReader(payload))
	if err != nil {
		return writeError(w, http.StatusBadGateway, "upstream_unavailable"), 0
	}
	request.Header.Set("Content-Type", "application/json")
	request.Header.Set("Authorization", "Bearer "+p.cfg.upstreamKey)
	response, err := p.client.Do(request)
	if err != nil {
		return writeError(w, http.StatusBadGateway, "upstream_unavailable"), 0
	}
	defer response.Body.Close()
	body, err := io.ReadAll(io.LimitReader(response.Body, maxUpstreamBodyBytes))
	if err != nil {
		return writeError(w, http.StatusBadGateway, "upstream_unavailable"), 0
	}
	if response.StatusCode != http.StatusOK {
		p.logger.Warn("upstream", "status", response.StatusCode)
		return writeError(w, http.StatusBadGateway, "upstream_error"), 0
	}
	var usage upstreamUsage
	if err := json.Unmarshal(body, &usage); err != nil {
		return writeError(w, http.StatusBadGateway, "upstream_error"), 0
	}
	p.budget.spend(usage.Usage.TotalTokens)
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	_, _ = w.Write(body)
	return http.StatusOK, usage.Usage.TotalTokens
}

func writeError(w http.ResponseWriter, status int, code string) int {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(map[string]map[string]string{"error": {"code": code}})
	return status
}
