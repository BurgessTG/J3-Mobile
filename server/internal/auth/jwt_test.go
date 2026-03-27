package auth

import (
	"net/http"
	"net/http/httptest"
	"path/filepath"
	"testing"
	"time"

	"github.com/golang-jwt/jwt/v5"

	"github.com/BurgessTG/J3-Mobile/server/internal/pairtoken"
)

const testSecret = "test-secret-key"

func TestSignAndParseToken(t *testing.T) {
	token, err := SignToken(testSecret, "user-123")
	if err != nil {
		t.Fatalf("SignToken failed: %v", err)
	}
	if token == "" {
		t.Fatal("SignToken returned empty token")
	}

	claims, err := ParseToken(testSecret, token)
	if err != nil {
		t.Fatalf("ParseToken failed: %v", err)
	}
	if claims.UserID != "user-123" {
		t.Errorf("expected UserID %q, got %q", "user-123", claims.UserID)
	}
}

func TestParseTokenRejectsExpired(t *testing.T) {
	now := time.Now()
	claims := Claims{
		UserID: "user-expired",
		RegisteredClaims: jwt.RegisteredClaims{
			IssuedAt:  jwt.NewNumericDate(now.Add(-3 * time.Hour)),
			ExpiresAt: jwt.NewNumericDate(now.Add(-2 * time.Hour)),
		},
	}

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	signed, err := token.SignedString([]byte(testSecret))
	if err != nil {
		t.Fatalf("failed to sign expired token: %v", err)
	}

	_, err = ParseToken(testSecret, signed)
	if err == nil {
		t.Fatal("expected ParseToken to reject expired token, but it succeeded")
	}
}

func TestParseTokenRejectsWrongSecret(t *testing.T) {
	token, err := SignToken("correct-secret", "user-123")
	if err != nil {
		t.Fatalf("SignToken failed: %v", err)
	}

	_, err = ParseToken("wrong-secret", token)
	if err == nil {
		t.Fatal("expected ParseToken to reject token signed with wrong secret, but it succeeded")
	}
}

func TestMiddlewareRejectsMissingAuth(t *testing.T) {
	handler := (&Authenticator{}).Middleware()(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	}))

	req := httptest.NewRequest(http.MethodGet, "/", nil)
	rec := httptest.NewRecorder()
	handler.ServeHTTP(rec, req)

	if rec.Code != http.StatusUnauthorized {
		t.Errorf("expected status %d, got %d", http.StatusUnauthorized, rec.Code)
	}
}

func TestMiddlewareAcceptsPairingToken(t *testing.T) {
	store := pairtoken.NewStore(filepath.Join(t.TempDir(), "tokens.json"))
	credential, token, err := store.Create("Jacob iPhone")
	if err != nil {
		t.Fatalf("Create failed: %v", err)
	}

	var gotCredentialID string
	var gotDeviceName string
	var gotMode AuthMode

	handler := (&Authenticator{PairingTokens: store}).Middleware()(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		gotCredentialID = CredentialIDFromContext(r.Context())
		gotDeviceName = DeviceNameFromContext(r.Context())
		gotMode = ModeFromContext(r.Context())
		w.WriteHeader(http.StatusOK)
	}))

	req := httptest.NewRequest(http.MethodGet, "/", nil)
	req.Header.Set("Authorization", "Bearer "+token)
	rec := httptest.NewRecorder()
	handler.ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("expected status 200, got %d", rec.Code)
	}
	if gotCredentialID != credential.ID {
		t.Fatalf("expected credential ID %q, got %q", credential.ID, gotCredentialID)
	}
	if gotDeviceName != credential.DeviceName {
		t.Fatalf("expected device name %q, got %q", credential.DeviceName, gotDeviceName)
	}
	if gotMode != AuthModePairingToken {
		t.Fatalf("expected auth mode %q, got %q", AuthModePairingToken, gotMode)
	}
}

func TestMiddlewareRejectsJWTByDefault(t *testing.T) {
	token, err := SignToken(testSecret, "user-ctx")
	if err != nil {
		t.Fatalf("SignToken failed: %v", err)
	}

	handler := (&Authenticator{JWTSecret: testSecret}).Middleware()(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	}))

	req := httptest.NewRequest(http.MethodGet, "/", nil)
	req.Header.Set("Authorization", "Bearer "+token)
	rec := httptest.NewRecorder()
	handler.ServeHTTP(rec, req)

	if rec.Code != http.StatusUnauthorized {
		t.Fatalf("expected JWT to be rejected by default, got status %d", rec.Code)
	}
}

func TestMiddlewareAcceptsLegacyJWTWhenEnabled(t *testing.T) {
	token, err := SignToken(testSecret, "user-ctx")
	if err != nil {
		t.Fatalf("SignToken failed: %v", err)
	}

	var gotCredentialID string
	var gotMode AuthMode
	handler := (&Authenticator{
		JWTSecret:      testSecret,
		AllowLegacyJWT: true,
	}).Middleware()(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		gotCredentialID = CredentialIDFromContext(r.Context())
		gotMode = ModeFromContext(r.Context())
		w.WriteHeader(http.StatusOK)
	}))

	req := httptest.NewRequest(http.MethodGet, "/", nil)
	req.Header.Set("Authorization", "Bearer "+token)
	rec := httptest.NewRecorder()
	handler.ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("expected status 200, got %d", rec.Code)
	}
	if gotCredentialID != "legacy-jwt:user-ctx" {
		t.Fatalf("expected legacy credential ID, got %q", gotCredentialID)
	}
	if gotMode != AuthModeLegacyJWT {
		t.Fatalf("expected auth mode %q, got %q", AuthModeLegacyJWT, gotMode)
	}
}
