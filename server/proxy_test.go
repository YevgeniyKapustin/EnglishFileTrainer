package main

import (
	"encoding/json"
	"io"
	"log/slog"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"
)

const (
	testAppKey      = "app-key-current"
	testOldAppKey   = "app-key-previous"
	testUpstreamKey = "upstream-secret"
	okMessages      = `{"messages":[{"role":"user","content":"Hi"}]}`
)

type fakeUpstream struct {
	server   *httptest.Server
	status   int
	tokens   int64
	received []map[string]any
	auth     []string
}

func newFakeUpstream(t *testing.T) *fakeUpstream {
	upstream := &fakeUpstream{status: http.StatusOK, tokens: 10}
	upstream.server = httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		var body map[string]any
		_ = json.NewDecoder(r.Body).Decode(&body)
		upstream.received = append(upstream.received, body)
		upstream.auth = append(upstream.auth, r.Header.Get("Authorization"))
		w.WriteHeader(upstream.status)
		if upstream.status != http.StatusOK {
			_, _ = io.WriteString(w, `{"error":{"message":"invalid key upstream-secret"}}`)
			return
		}
		_ = json.NewEncoder(w).Encode(map[string]any{
			"choices": []any{map[string]any{"message": map[string]any{"role": "assistant", "content": "Hello"}}},
			"usage":   map[string]any{"total_tokens": upstream.tokens},
		})
	}))
	t.Cleanup(upstream.server.Close)
	return upstream
}

func testConfig(t *testing.T, upstreamURL string, overrides map[string]string) config {
	env := map[string]string{
		"DEEPSEEK_API_KEY": testUpstreamKey,
		"APP_KEYS":         testAppKey + ", " + testOldAppKey,
		"UPSTREAM_URL":     upstreamURL,
		"MAX_TOKENS":       "500",
		"MAX_BODY_BYTES":   "1024",
	}
	for name, value := range overrides {
		env[name] = value
	}
	cfg, err := loadConfig(func(name string) string { return env[name] })
	if err != nil {
		t.Fatal(err)
	}
	return cfg
}

func newTestHandler(t *testing.T, upstream *fakeUpstream, overrides map[string]string, now func() time.Time) http.Handler {
	cfg := testConfig(t, upstream.server.URL, overrides)
	logger := slog.New(slog.NewTextHandler(io.Discard, nil))
	return newProxy(cfg, upstream.server.Client(), logger, now).routes()
}

func chat(handler http.Handler, appKey, body string) *httptest.ResponseRecorder {
	request := httptest.NewRequest(http.MethodPost, "/v1/chat", strings.NewReader(body))
	if appKey != "" {
		request.Header.Set("Authorization", "Bearer "+appKey)
	}
	recorder := httptest.NewRecorder()
	handler.ServeHTTP(recorder, request)
	return recorder
}

func TestRejectsMissingAndUnknownKeys(t *testing.T) {
	upstream := newFakeUpstream(t)
	handler := newTestHandler(t, upstream, nil, time.Now)
	for _, appKey := range []string{"", "wrong", testAppKey + "x"} {
		if got := chat(handler, appKey, okMessages).Code; got != http.StatusUnauthorized {
			t.Fatalf("key %q: status %d, want 401", appKey, got)
		}
	}
	if len(upstream.received) != 0 {
		t.Fatal("unauthorized request reached upstream")
	}
}

func TestAcceptsEveryConfiguredKey(t *testing.T) {
	upstream := newFakeUpstream(t)
	handler := newTestHandler(t, upstream, nil, time.Now)
	for _, appKey := range []string{testAppKey, testOldAppKey} {
		if got := chat(handler, appKey, okMessages).Code; got != http.StatusOK {
			t.Fatalf("key %q: status %d, want 200", appKey, got)
		}
	}
}

func TestForcesModelAndCapsMaxTokens(t *testing.T) {
	upstream := newFakeUpstream(t)
	handler := newTestHandler(t, upstream, nil, time.Now)
	body := `{"model":"gpt-4o","max_tokens":9000,"temperature":0.3,"messages":[{"role":"user","content":"Hi"}]}`
	response := chat(handler, testAppKey, body)
	if response.Code != http.StatusOK {
		t.Fatalf("status %d, want 200", response.Code)
	}
	sent := upstream.received[0]
	if sent["model"] != upstreamModel {
		t.Fatalf("model %v, want %s", sent["model"], upstreamModel)
	}
	if sent["max_tokens"] != float64(500) {
		t.Fatalf("max_tokens %v, want 500", sent["max_tokens"])
	}
	if sent["temperature"] != 0.3 {
		t.Fatalf("temperature %v, want 0.3", sent["temperature"])
	}
	if upstream.auth[0] != "Bearer "+testUpstreamKey {
		t.Fatalf("upstream auth %q", upstream.auth[0])
	}
	if !strings.Contains(response.Body.String(), `"content":"Hello"`) {
		t.Fatalf("body %s is not the upstream response", response.Body.String())
	}
}

func TestKeepsSmallerMaxTokensAndDefaultsMissing(t *testing.T) {
	upstream := newFakeUpstream(t)
	handler := newTestHandler(t, upstream, nil, time.Now)
	chat(handler, testAppKey, `{"max_tokens":5,"messages":[{"role":"user","content":"Hi"}]}`)
	chat(handler, testAppKey, okMessages)
	if upstream.received[0]["max_tokens"] != float64(5) {
		t.Fatalf("max_tokens %v, want 5", upstream.received[0]["max_tokens"])
	}
	if upstream.received[1]["max_tokens"] != float64(500) {
		t.Fatalf("max_tokens %v, want 500", upstream.received[1]["max_tokens"])
	}
}

func TestRateLimitPerAppKey(t *testing.T) {
	upstream := newFakeUpstream(t)
	handler := newTestHandler(t, upstream, map[string]string{"RATE_PER_MIN": "2"}, time.Now)
	for range 2 {
		if got := chat(handler, testAppKey, okMessages).Code; got != http.StatusOK {
			t.Fatalf("status %d, want 200", got)
		}
	}
	if got := chat(handler, testAppKey, okMessages).Code; got != http.StatusTooManyRequests {
		t.Fatalf("status %d, want 429", got)
	}
	if got := chat(handler, testOldAppKey, okMessages).Code; got != http.StatusOK {
		t.Fatalf("other key status %d, want 200", got)
	}
}

func TestDailyTokenLimitResetsAtUtcMidnight(t *testing.T) {
	upstream := newFakeUpstream(t)
	upstream.tokens = 60
	now := time.Date(2026, 9, 29, 23, 0, 0, 0, time.UTC)
	handler := newTestHandler(t, upstream, map[string]string{"DAILY_TOKEN_LIMIT": "100"}, func() time.Time { return now })
	for range 2 {
		if got := chat(handler, testAppKey, okMessages).Code; got != http.StatusOK {
			t.Fatalf("status %d, want 200", got)
		}
	}
	if got := chat(handler, testAppKey, okMessages).Code; got != http.StatusTooManyRequests {
		t.Fatalf("status %d, want 429", got)
	}
	now = now.Add(2 * time.Hour)
	if got := chat(handler, testAppKey, okMessages).Code; got != http.StatusOK {
		t.Fatalf("after midnight status %d, want 200", got)
	}
}

func TestRejectsOversizedBody(t *testing.T) {
	upstream := newFakeUpstream(t)
	handler := newTestHandler(t, upstream, nil, time.Now)
	body := `{"messages":[{"role":"user","content":"` + strings.Repeat("a", 2048) + `"}]}`
	if got := chat(handler, testAppKey, body).Code; got != http.StatusRequestEntityTooLarge {
		t.Fatalf("status %d, want 413", got)
	}
}

func TestRejectsMalformedBody(t *testing.T) {
	upstream := newFakeUpstream(t)
	handler := newTestHandler(t, upstream, nil, time.Now)
	for _, body := range []string{"not json", `{"temperature":1}`} {
		if got := chat(handler, testAppKey, body).Code; got != http.StatusBadRequest {
			t.Fatalf("body %q: status %d, want 400", body, got)
		}
	}
}

func TestRejectsWrongMethod(t *testing.T) {
	upstream := newFakeUpstream(t)
	handler := newTestHandler(t, upstream, nil, time.Now)
	recorder := httptest.NewRecorder()
	handler.ServeHTTP(recorder, httptest.NewRequest(http.MethodGet, "/v1/chat", nil))
	if recorder.Code != http.StatusMethodNotAllowed {
		t.Fatalf("status %d, want 405", recorder.Code)
	}
}

func TestUpstreamErrorBecomesBadGatewayWithoutLeak(t *testing.T) {
	upstream := newFakeUpstream(t)
	upstream.status = http.StatusUnauthorized
	handler := newTestHandler(t, upstream, nil, time.Now)
	response := chat(handler, testAppKey, okMessages)
	if response.Code != http.StatusBadGateway {
		t.Fatalf("status %d, want 502", response.Code)
	}
	if strings.Contains(response.Body.String(), testUpstreamKey) {
		t.Fatal("upstream key leaked to client")
	}
}

func TestUnreachableUpstreamBecomesBadGateway(t *testing.T) {
	upstream := newFakeUpstream(t)
	handler := newTestHandler(t, upstream, nil, time.Now)
	upstream.server.Close()
	if got := chat(handler, testAppKey, okMessages).Code; got != http.StatusBadGateway {
		t.Fatalf("status %d, want 502", got)
	}
}

func TestHealth(t *testing.T) {
	upstream := newFakeUpstream(t)
	handler := newTestHandler(t, upstream, nil, time.Now)
	recorder := httptest.NewRecorder()
	handler.ServeHTTP(recorder, httptest.NewRequest(http.MethodGet, "/healthz", nil))
	if recorder.Code != http.StatusOK {
		t.Fatalf("status %d, want 200", recorder.Code)
	}
}

func TestConfigRequiresSecrets(t *testing.T) {
	for _, env := range []map[string]string{
		{"APP_KEYS": testAppKey},
		{"DEEPSEEK_API_KEY": testUpstreamKey},
		{"DEEPSEEK_API_KEY": testUpstreamKey, "APP_KEYS": " , "},
		{"DEEPSEEK_API_KEY": testUpstreamKey, "APP_KEYS": testAppKey, "RATE_PER_MIN": "0"},
	} {
		if _, err := loadConfig(func(name string) string { return env[name] }); err == nil {
			t.Fatalf("env %v: expected error", env)
		}
	}
}
