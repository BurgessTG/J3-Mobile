package devtoken

import (
	"fmt"

	"github.com/BurgessTG/J3-Mobile/server/internal/auth"
)

// Generate creates a signed JWT for local development.
func Generate(secret, userID string) (string, error) {
	if secret == "" {
		return "", fmt.Errorf("JWT_SECRET is required but not set")
	}
	if userID == "" {
		return "", fmt.Errorf("user id is required")
	}

	return auth.SignToken(secret, userID)
}
