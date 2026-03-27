package config

import "testing"

func TestLoad_MissingUpstreamURL(t *testing.T) {
	t.Setenv("T3_UPSTREAM_URL", "")

	_, err := Load()
	if err == nil {
		t.Fatal("expected error when T3_UPSTREAM_URL is empty, got nil")
	}
}

func TestLoad_DefaultListenAddr(t *testing.T) {
	t.Setenv("T3_UPSTREAM_URL", "ws://localhost:3000")
	t.Setenv("LISTEN_ADDR", "")
	t.Setenv("PORT", "")
	t.Setenv("ALLOW_LEGACY_JWT", "")

	cfg, err := Load()
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if cfg.ListenAddr != "127.0.0.1:8181" {
		t.Fatalf("expected default listen addr 127.0.0.1:8181, got %s", cfg.ListenAddr)
	}
}

func TestLoad_UsesLegacyPortOnLoopback(t *testing.T) {
	t.Setenv("T3_UPSTREAM_URL", "ws://localhost:3000")
	t.Setenv("LISTEN_ADDR", "")
	t.Setenv("PORT", "9090")
	t.Setenv("ALLOW_LEGACY_JWT", "")

	cfg, err := Load()
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if cfg.ListenAddr != "127.0.0.1:9090" {
		t.Fatalf("expected loopback listen addr using PORT, got %s", cfg.ListenAddr)
	}
}

func TestLoad_RequiresJWTSecretWhenLegacyJWTEnabled(t *testing.T) {
	t.Setenv("T3_UPSTREAM_URL", "ws://localhost:3000")
	t.Setenv("ALLOW_LEGACY_JWT", "true")
	t.Setenv("JWT_SECRET", "")

	_, err := Load()
	if err == nil {
		t.Fatal("expected error when legacy JWT auth is enabled without JWT_SECRET")
	}
}

func TestLoad_NormalizesUpstreamURLAndExtractsToken(t *testing.T) {
	t.Setenv("LISTEN_ADDR", "127.0.0.1:8181")
	t.Setenv("T3_UPSTREAM_URL", "ws://example.com:3000/socket?token=query-token&foo=bar")
	t.Setenv("T3_AUTH_TOKEN", "")
	t.Setenv("ALLOW_LEGACY_JWT", "")
	t.Setenv("PUBLIC_BASE_URL", "https://bridge.example.ts.net/")

	cfg, err := Load()
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	if cfg.UpstreamURL != "ws://example.com:3000/socket?foo=bar" {
		t.Fatalf("unexpected normalized upstream URL: %s", cfg.UpstreamURL)
	}
	if cfg.UpstreamAuthToken != "query-token" {
		t.Fatalf("expected token from query string, got %q", cfg.UpstreamAuthToken)
	}
	if cfg.UpstreamLogURL != "ws://example.com:3000/socket?foo=bar" {
		t.Fatalf("unexpected upstream log URL: %s", cfg.UpstreamLogURL)
	}
	if cfg.PublicBaseURL != "https://bridge.example.ts.net" {
		t.Fatalf("expected trimmed PUBLIC_BASE_URL, got %q", cfg.PublicBaseURL)
	}
}

func TestLoad_PrefersExplicitAuthToken(t *testing.T) {
	t.Setenv("LISTEN_ADDR", "127.0.0.1:8181")
	t.Setenv("T3_UPSTREAM_URL", "ws://example.com:3000/socket?token=query-token")
	t.Setenv("T3_AUTH_TOKEN", "env-token")
	t.Setenv("ALLOW_LEGACY_JWT", "")

	cfg, err := Load()
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	if cfg.UpstreamAuthToken != "env-token" {
		t.Fatalf("expected explicit auth token to win, got %q", cfg.UpstreamAuthToken)
	}
	if cfg.UpstreamURL != "ws://example.com:3000/socket" {
		t.Fatalf("expected sanitized upstream URL, got %q", cfg.UpstreamURL)
	}
}
