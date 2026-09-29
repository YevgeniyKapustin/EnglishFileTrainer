package main

import (
	"sync"
	"time"

	"golang.org/x/time/rate"
)

type keyLimiters struct {
	mu         sync.Mutex
	perMinute  int
	byAppKeyID map[int]*rate.Limiter
}

func newKeyLimiters(perMinute int) *keyLimiters {
	return &keyLimiters{perMinute: perMinute, byAppKeyID: map[int]*rate.Limiter{}}
}

func (l *keyLimiters) allow(appKeyID int) bool {
	l.mu.Lock()
	limiter, ok := l.byAppKeyID[appKeyID]
	if !ok {
		limiter = rate.NewLimiter(rate.Every(time.Minute/time.Duration(l.perMinute)), l.perMinute)
		l.byAppKeyID[appKeyID] = limiter
	}
	l.mu.Unlock()
	return limiter.Allow()
}

type dailyBudget struct {
	mu    sync.Mutex
	limit int64
	used  int64
	day   string
	now   func() time.Time
}

func newDailyBudget(limit int64, now func() time.Time) *dailyBudget {
	return &dailyBudget{limit: limit, now: now}
}

func (b *dailyBudget) exhausted() bool {
	b.mu.Lock()
	defer b.mu.Unlock()
	b.rollOver()
	return b.used >= b.limit
}

func (b *dailyBudget) spend(tokens int64) {
	b.mu.Lock()
	defer b.mu.Unlock()
	b.rollOver()
	b.used += tokens
}

func (b *dailyBudget) rollOver() {
	today := b.now().UTC().Format(time.DateOnly)
	if today != b.day {
		b.day = today
		b.used = 0
	}
}
