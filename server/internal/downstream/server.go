package downstream

import (
	"net/url"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"

	"github.com/BurgessTG/J3-Mobile/server/internal/auth"
	"github.com/BurgessTG/J3-Mobile/server/internal/bridge"
)

type RouterConfig struct {
	Authenticator *auth.Authenticator
	ListenAddr    string
	PublicBaseURL string
}

// NewRouter creates a chi router with all downstream endpoints for mobile clients.
func NewRouter(b *bridge.Bridge, cfg RouterConfig) *chi.Mux {
	r := chi.NewRouter()

	r.Use(middleware.RequestID)
	r.Use(middleware.RealIP)
	r.Use(middleware.Recoverer)

	// Public routes.
	r.Get("/health", healthHandler(b, cfg))

	// Authenticated routes.
	r.Group(func(r chi.Router) {
		r.Use(cfg.Authenticator.Middleware())
		r.Get("/snapshot", snapshotHandler(b))
		r.Get("/sessions", sessionsHandler(b))
		r.Get("/ws", wsHandler(b, allowedOrigin(cfg.PublicBaseURL)))
	})

	return r
}

func allowedOrigin(publicBaseURL string) string {
	if publicBaseURL == "" {
		return ""
	}

	parsed, err := url.Parse(publicBaseURL)
	if err != nil {
		return ""
	}

	return parsed.Scheme + "://" + parsed.Host
}
