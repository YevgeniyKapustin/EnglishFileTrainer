package main

import (
	"errors"
	"fmt"
	"strconv"
	"strings"
)

const (
	defaultPort            = "8080"
	defaultRatePerMin      = 20
	defaultDailyTokenLimit = 2_000_000
	defaultMaxBodyBytes    = 256 << 10
	defaultMaxTokens       = 2048
	defaultUpstreamURL     = "https://api.deepseek.com/chat/completions"
)

type config struct {
	upstreamKey     string
	appKeys         []string
	host            string
	port            string
	ratePerMin      int
	dailyTokenLimit int64
	maxBodyBytes    int64
	maxTokens       int
	upstreamURL     string
}

func loadConfig(getenv func(string) string) (config, error) {
	cfg := config{
		upstreamKey: strings.TrimSpace(getenv("DEEPSEEK_API_KEY")),
		appKeys:     splitKeys(getenv("APP_KEYS")),
		host:        strings.TrimSpace(getenv("HOST")),
		port:        valueOr(getenv("PORT"), defaultPort),
		upstreamURL: valueOr(getenv("UPSTREAM_URL"), defaultUpstreamURL),
	}
	if cfg.upstreamKey == "" {
		return config{}, errors.New("DEEPSEEK_API_KEY is required")
	}
	if len(cfg.appKeys) == 0 {
		return config{}, errors.New("APP_KEYS is required")
	}
	var err error
	if cfg.ratePerMin, err = positiveInt(getenv, "RATE_PER_MIN", defaultRatePerMin); err != nil {
		return config{}, err
	}
	if cfg.maxTokens, err = positiveInt(getenv, "MAX_TOKENS", defaultMaxTokens); err != nil {
		return config{}, err
	}
	dailyTokenLimit, err := positiveInt(getenv, "DAILY_TOKEN_LIMIT", defaultDailyTokenLimit)
	if err != nil {
		return config{}, err
	}
	maxBodyBytes, err := positiveInt(getenv, "MAX_BODY_BYTES", defaultMaxBodyBytes)
	if err != nil {
		return config{}, err
	}
	cfg.dailyTokenLimit = int64(dailyTokenLimit)
	cfg.maxBodyBytes = int64(maxBodyBytes)
	return cfg, nil
}

func splitKeys(raw string) []string {
	var keys []string
	for _, key := range strings.Split(raw, ",") {
		if trimmed := strings.TrimSpace(key); trimmed != "" {
			keys = append(keys, trimmed)
		}
	}
	return keys
}

func valueOr(value, fallback string) string {
	if trimmed := strings.TrimSpace(value); trimmed != "" {
		return trimmed
	}
	return fallback
}

func positiveInt(getenv func(string) string, name string, fallback int) (int, error) {
	raw := strings.TrimSpace(getenv(name))
	if raw == "" {
		return fallback, nil
	}
	value, err := strconv.Atoi(raw)
	if err != nil || value <= 0 {
		return 0, fmt.Errorf("%s must be a positive integer", name)
	}
	return value, nil
}
