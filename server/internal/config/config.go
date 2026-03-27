package config

import (
	"fmt"
	"net"
	"net/url"
	"os"
	"strings"
)

type Config struct {
	ListenAddr        string
	PublicBaseURL     string
	UpstreamURL       string
	UpstreamAuthToken string
	UpstreamLogURL    string
	JWTSecret         string
	AllowLegacyJWT    bool
	LogLevel          string
}

func Load() (*Config, error) {
	upstreamURL := strings.TrimSpace(os.Getenv("T3_UPSTREAM_URL"))
	if upstreamURL == "" {
		return nil, fmt.Errorf("T3_UPSTREAM_URL is required but not set")
	}

	allowLegacyJWT := parseBoolEnv("ALLOW_LEGACY_JWT")
	jwtSecret := strings.TrimSpace(os.Getenv("JWT_SECRET"))
	if allowLegacyJWT && jwtSecret == "" {
		return nil, fmt.Errorf("JWT_SECRET is required when ALLOW_LEGACY_JWT=true")
	}

	normalizedUpstreamURL, upstreamAuthToken, upstreamLogURL, err := normalizeUpstream(upstreamURL, strings.TrimSpace(os.Getenv("T3_AUTH_TOKEN")))
	if err != nil {
		return nil, err
	}

	listenAddr, err := resolveListenAddr(os.Getenv("LISTEN_ADDR"), os.Getenv("PORT"))
	if err != nil {
		return nil, err
	}

	publicBaseURL := strings.TrimSpace(os.Getenv("PUBLIC_BASE_URL"))
	if publicBaseURL != "" {
		if _, err := url.Parse(publicBaseURL); err != nil {
			return nil, fmt.Errorf("invalid PUBLIC_BASE_URL: %w", err)
		}
		publicBaseURL = strings.TrimRight(publicBaseURL, "/")
	}

	logLevel := strings.TrimSpace(os.Getenv("LOG_LEVEL"))
	if logLevel == "" {
		logLevel = "info"
	}

	return &Config{
		ListenAddr:        listenAddr,
		PublicBaseURL:     publicBaseURL,
		UpstreamURL:       normalizedUpstreamURL,
		UpstreamAuthToken: upstreamAuthToken,
		UpstreamLogURL:    upstreamLogURL,
		JWTSecret:         jwtSecret,
		AllowLegacyJWT:    allowLegacyJWT,
		LogLevel:          logLevel,
	}, nil
}

func resolveListenAddr(rawListenAddr, rawPort string) (string, error) {
	listenAddr := strings.TrimSpace(rawListenAddr)
	if listenAddr == "" {
		port := strings.TrimSpace(rawPort)
		if port == "" {
			port = "8181"
		}
		listenAddr = net.JoinHostPort("127.0.0.1", port)
	}

	if _, _, err := net.SplitHostPort(listenAddr); err != nil {
		return "", fmt.Errorf("invalid LISTEN_ADDR %q: %w", listenAddr, err)
	}

	return listenAddr, nil
}

func normalizeUpstream(rawURL, envToken string) (cleanedURL string, authToken string, logURL string, err error) {
	parsed, err := url.Parse(strings.TrimSpace(rawURL))
	if err != nil {
		return "", "", "", fmt.Errorf("invalid T3_UPSTREAM_URL: %w", err)
	}

	query := parsed.Query()
	queryToken := strings.TrimSpace(query.Get("token"))
	query.Del("token")
	parsed.RawQuery = query.Encode()
	parsed.User = nil

	authToken = strings.TrimSpace(envToken)
	if authToken == "" {
		authToken = queryToken
	}

	cleanedURL = parsed.String()
	logURL = redactURL(parsed)
	return cleanedURL, authToken, logURL, nil
}

func redactURL(parsed *url.URL) string {
	clone := *parsed
	clone.User = nil
	query := clone.Query()
	for _, key := range []string{"token", "access_token", "auth", "authorization"} {
		if query.Has(key) {
			query.Set(key, "REDACTED")
		}
	}
	clone.RawQuery = query.Encode()
	return clone.String()
}

func parseBoolEnv(key string) bool {
	value := strings.TrimSpace(strings.ToLower(os.Getenv(key)))
	return value == "1" || value == "true" || value == "yes" || value == "on"
}
