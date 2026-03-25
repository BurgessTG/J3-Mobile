package downstream

import (
	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
	"github.com/rs/cors"

	"github.com/BurgessTG/J3-Mobile/server/internal/auth"
	"github.com/BurgessTG/J3-Mobile/server/internal/bridge"
)

// NewRouter creates a chi router with all downstream endpoints for mobile clients.
func NewRouter(b *bridge.Bridge, jwtSecret string) *chi.Mux {
	r := chi.NewRouter()

	r.Use(middleware.RequestID)
	r.Use(middleware.RealIP)
	r.Use(middleware.Recoverer)
	r.Use(cors.AllowAll().Handler)

	// Public routes.
	r.Get("/health", healthHandler(b))

	// Authenticated routes.
	r.Group(func(r chi.Router) {
		r.Use(auth.Middleware(jwtSecret))
		r.Get("/snapshot", snapshotHandler(b))
		r.Get("/sessions", sessionsHandler(b))
		r.Get("/ws", wsHandler(b))
	})

	return r
}
