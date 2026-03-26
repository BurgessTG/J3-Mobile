package main

import (
	"context"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/BurgessTG/J3-Mobile/server/internal/auth"
	"github.com/BurgessTG/J3-Mobile/server/internal/bridge"
	"github.com/BurgessTG/J3-Mobile/server/internal/config"
	"github.com/BurgessTG/J3-Mobile/server/internal/downstream"
	"github.com/BurgessTG/J3-Mobile/server/internal/pairtoken"
	"github.com/BurgessTG/J3-Mobile/server/internal/upstream"
	"github.com/go-chi/chi/v5/middleware"
	"github.com/joho/godotenv"
	"github.com/rs/zerolog"
	"github.com/rs/zerolog/log"
)

func main() {
	_ = godotenv.Load()

	cfg, err := config.Load()
	if err != nil {
		log.Fatal().Err(err).Msg("failed to load config")
	}

	level, err := zerolog.ParseLevel(cfg.LogLevel)
	if err != nil {
		level = zerolog.InfoLevel
	}
	zerolog.SetGlobalLevel(level)
	logger := zerolog.New(os.Stdout).With().Timestamp().Logger()
	log.Logger = logger

	tokenStorePath, err := pairtoken.DefaultPath()
	if err != nil {
		logger.Fatal().Err(err).Msg("failed to resolve pair token store path")
	}

	authenticator := &auth.Authenticator{
		PairingTokens:  pairtoken.NewStore(tokenStorePath),
		JWTSecret:      cfg.JWTSecret,
		AllowLegacyJWT: cfg.AllowLegacyJWT,
	}

	logger.Info().
		Str("listen_addr", cfg.ListenAddr).
		Str("public_base_url", cfg.PublicBaseURL).
		Str("upstream_url", cfg.UpstreamLogURL).
		Str("auth_mode", authenticator.HealthMode()).
		Str("token_store", tokenStorePath).
		Msg("J3 Bridge starting")

	upstreamClient := upstream.NewClient(cfg.UpstreamURL, cfg.UpstreamAuthToken, logger)
	bridgeServer := bridge.New(upstreamClient, logger)

	startCtx, startCancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer startCancel()

	if err := bridgeServer.Start(startCtx); err != nil {
		logger.Error().Err(err).Msg("bridge startup failed")
		if closeErr := upstreamClient.Close(); closeErr != nil {
			logger.Warn().Err(closeErr).Msg("failed to close upstream client")
		}
		os.Exit(1)
	}
	defer func() {
		if err := upstreamClient.Close(); err != nil {
			logger.Warn().Err(err).Msg("failed to close upstream client")
		}
	}()

	r := downstream.NewRouter(bridgeServer, downstream.RouterConfig{
		Authenticator: authenticator,
		ListenAddr:    cfg.ListenAddr,
		PublicBaseURL: cfg.PublicBaseURL,
	})
	handler := requestLogger(logger)(r)

	srv := &http.Server{
		Addr:              cfg.ListenAddr,
		Handler:           handler,
		ReadHeaderTimeout: 10 * time.Second,
		ReadTimeout:       30 * time.Second,
		WriteTimeout:      30 * time.Second,
		IdleTimeout:       60 * time.Second,
	}

	done := make(chan os.Signal, 1)
	signal.Notify(done, syscall.SIGINT, syscall.SIGTERM)

	errCh := make(chan error, 1)
	go func() {
		errCh <- srv.ListenAndServe()
	}()

	select {
	case <-done:
		logger.Info().Msg("shutting down")
	case err := <-errCh:
		if err != nil && err != http.ErrServerClosed {
			logger.Error().Err(err).Msg("server failed")
			if closeErr := upstreamClient.Close(); closeErr != nil {
				logger.Warn().Err(closeErr).Msg("failed to close upstream client")
			}
			os.Exit(1)
		}
	}

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	if err := srv.Shutdown(ctx); err != nil {
		logger.Error().Err(err).Msg("shutdown failed")
		if closeErr := upstreamClient.Close(); closeErr != nil {
			logger.Warn().Err(closeErr).Msg("failed to close upstream client")
		}
		os.Exit(1)
	}

	logger.Info().Msg("server stopped")
}

func requestLogger(logger zerolog.Logger) func(next http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			start := time.Now()
			ww := middleware.NewWrapResponseWriter(w, r.ProtoMajor)
			next.ServeHTTP(ww, r)
			logger.Info().
				Str("method", r.Method).
				Str("path", r.URL.Path).
				Int("status", ww.Status()).
				Dur("duration", time.Since(start)).
				Msg("request")
		})
	}
}
