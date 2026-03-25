package config

import (
	"testing"
)

func TestLoad_MissingUpstreamURL(t *testing.T) {
	t.Setenv("T3_UPSTREAM_URL", "")
	t.Setenv("JWT_SECRET", "testsecret")

	_, err := Load()
	if err == nil {
		t.Fatal("expected error when T3_UPSTREAM_URL is empty, got nil")
	}
}

func TestLoad_MissingJWTSecret(t *testing.T) {
	t.Setenv("T3_UPSTREAM_URL", "ws://localhost:3000")
	t.Setenv("JWT_SECRET", "")

	_, err := Load()
	if err == nil {
		t.Fatal("expected error when JWT_SECRET is empty, got nil")
	}
}

func TestLoad_DefaultPort(t *testing.T) {
	t.Setenv("T3_UPSTREAM_URL", "ws://localhost:3000")
	t.Setenv("JWT_SECRET", "testsecret")
	t.Setenv("PORT", "")

	cfg, err := Load()
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if cfg.Port != "8181" {
		t.Errorf("expected default port 8181, got %s", cfg.Port)
	}
}

func TestLoad_AllEnvVars(t *testing.T) {
	t.Setenv("PORT", "9090")
	t.Setenv("T3_UPSTREAM_URL", "ws://example.com:3000")
	t.Setenv("T3_AUTH_TOKEN", "mytoken")
	t.Setenv("JWT_SECRET", "supersecret")
	t.Setenv("LOG_LEVEL", "debug")

	cfg, err := Load()
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	if cfg.Port != "9090" {
		t.Errorf("Port = %s, want 9090", cfg.Port)
	}
	if cfg.UpstreamURL != "ws://example.com:3000" {
		t.Errorf("UpstreamURL = %s, want ws://example.com:3000", cfg.UpstreamURL)
	}
	if cfg.UpstreamAuthToken != "mytoken" {
		t.Errorf("UpstreamAuthToken = %s, want mytoken", cfg.UpstreamAuthToken)
	}
	if cfg.JWTSecret != "supersecret" {
		t.Errorf("JWTSecret = %s, want supersecret", cfg.JWTSecret)
	}
	if cfg.LogLevel != "debug" {
		t.Errorf("LogLevel = %s, want debug", cfg.LogLevel)
	}
}
