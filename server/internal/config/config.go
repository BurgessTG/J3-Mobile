package config

import (
	"fmt"
	"os"
)

type Config struct {
	Port              string
	UpstreamURL       string
	UpstreamAuthToken string
	JWTSecret         string
	LogLevel          string
}

func Load() (*Config, error) {
	upstreamURL := os.Getenv("T3_UPSTREAM_URL")
	if upstreamURL == "" {
		return nil, fmt.Errorf("T3_UPSTREAM_URL is required but not set")
	}

	jwtSecret := os.Getenv("JWT_SECRET")
	if jwtSecret == "" {
		return nil, fmt.Errorf("JWT_SECRET is required but not set")
	}

	port := os.Getenv("PORT")
	if port == "" {
		port = "8181"
	}

	logLevel := os.Getenv("LOG_LEVEL")
	if logLevel == "" {
		logLevel = "info"
	}

	return &Config{
		Port:              port,
		UpstreamURL:       upstreamURL,
		UpstreamAuthToken: os.Getenv("T3_AUTH_TOKEN"),
		JWTSecret:         jwtSecret,
		LogLevel:          logLevel,
	}, nil
}
