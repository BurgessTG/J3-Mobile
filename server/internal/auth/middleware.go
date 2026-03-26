package auth

import (
	"context"
	"errors"
	"net/http"
	"strings"

	"github.com/BurgessTG/J3-Mobile/server/internal/pairtoken"
)

type contextKey string

const (
	credentialIDKey contextKey = "credentialId"
	deviceNameKey   contextKey = "deviceName"
	authModeKey     contextKey = "authMode"
)

type AuthMode string

const (
	AuthModePairingToken AuthMode = "pairing-token"
	AuthModeLegacyJWT    AuthMode = "legacy-jwt"
)

type Identity struct {
	CredentialID string
	DeviceName   string
	Mode         AuthMode
}

type Authenticator struct {
	PairingTokens  *pairtoken.Store
	JWTSecret      string
	AllowLegacyJWT bool
}

func (a *Authenticator) Middleware() func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			token, err := bearerToken(r.Header.Get("Authorization"))
			if err != nil {
				http.Error(w, err.Error(), http.StatusUnauthorized)
				return
			}

			identity, err := a.Authenticate(token)
			if err != nil {
				http.Error(w, "invalid or expired credential", http.StatusUnauthorized)
				return
			}

			ctx := context.WithValue(r.Context(), credentialIDKey, identity.CredentialID)
			ctx = context.WithValue(ctx, deviceNameKey, identity.DeviceName)
			ctx = context.WithValue(ctx, authModeKey, identity.Mode)
			next.ServeHTTP(w, r.WithContext(ctx))
		})
	}
}

func (a *Authenticator) Authenticate(token string) (*Identity, error) {
	token = strings.TrimSpace(token)
	if token == "" {
		return nil, errors.New("credential is required")
	}

	if a.PairingTokens != nil {
		credential, err := a.PairingTokens.Authenticate(token)
		if err == nil {
			return &Identity{
				CredentialID: credential.ID,
				DeviceName:   credential.DeviceName,
				Mode:         AuthModePairingToken,
			}, nil
		}
		if err != nil && !errors.Is(err, pairtoken.ErrInvalidToken) {
			return nil, err
		}
	}

	if a.AllowLegacyJWT {
		claims, err := ParseToken(a.JWTSecret, token)
		if err == nil {
			return &Identity{
				CredentialID: "legacy-jwt:" + claims.UserID,
				DeviceName:   "legacy-jwt",
				Mode:         AuthModeLegacyJWT,
			}, nil
		}
	}

	return nil, errors.New("credential rejected")
}

func (a *Authenticator) HealthMode() string {
	if a.AllowLegacyJWT {
		return string(AuthModePairingToken) + "+jwt-legacy"
	}
	return string(AuthModePairingToken)
}

func CredentialIDFromContext(ctx context.Context) string {
	value, _ := ctx.Value(credentialIDKey).(string)
	return value
}

func DeviceNameFromContext(ctx context.Context) string {
	value, _ := ctx.Value(deviceNameKey).(string)
	return value
}

func ModeFromContext(ctx context.Context) AuthMode {
	value, _ := ctx.Value(authModeKey).(AuthMode)
	return value
}

func bearerToken(header string) (string, error) {
	if header == "" {
		return "", errors.New("missing authorization header")
	}

	parts := strings.SplitN(strings.TrimSpace(header), " ", 2)
	if len(parts) != 2 || !strings.EqualFold(parts[0], "Bearer") || strings.TrimSpace(parts[1]) == "" {
		return "", errors.New("invalid authorization header format")
	}

	return strings.TrimSpace(parts[1]), nil
}
