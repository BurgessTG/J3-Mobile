package auth

import (
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/golang-jwt/jwt/v5"
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

func TestParseTokenReturnsCorrectUserID(t *testing.T) {
	ids := []string{"alice", "bob", "user-456", "00000000-0000-0000-0000-000000000001"}
	for _, id := range ids {
		token, err := SignToken(testSecret, id)
		if err != nil {
			t.Fatalf("SignToken(%q) failed: %v", id, err)
		}

		claims, err := ParseToken(testSecret, token)
		if err != nil {
			t.Fatalf("ParseToken for user %q failed: %v", id, err)
		}
		if claims.UserID != id {
			t.Errorf("expected UserID %q, got %q", id, claims.UserID)
		}
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
	handler := Middleware(testSecret)(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	}))

	req := httptest.NewRequest(http.MethodGet, "/", nil)
	rec := httptest.NewRecorder()
	handler.ServeHTTP(rec, req)

	if rec.Code != http.StatusUnauthorized {
		t.Errorf("expected status %d, got %d", http.StatusUnauthorized, rec.Code)
	}
}

func TestMiddlewareRejectsInvalidToken(t *testing.T) {
	handler := Middleware(testSecret)(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	}))

	req := httptest.NewRequest(http.MethodGet, "/", nil)
	req.Header.Set("Authorization", "Bearer not-a-valid-jwt")
	rec := httptest.NewRecorder()
	handler.ServeHTTP(rec, req)

	if rec.Code != http.StatusUnauthorized {
		t.Errorf("expected status %d, got %d", http.StatusUnauthorized, rec.Code)
	}
}

func TestMiddlewareSetsUserIDInContext(t *testing.T) {
	token, err := SignToken(testSecret, "user-ctx")
	if err != nil {
		t.Fatalf("SignToken failed: %v", err)
	}

	var gotUserID string
	handler := Middleware(testSecret)(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		gotUserID = UserIDFromContext(r.Context())
		w.WriteHeader(http.StatusOK)
	}))

	req := httptest.NewRequest(http.MethodGet, "/", nil)
	req.Header.Set("Authorization", "Bearer "+token)
	rec := httptest.NewRecorder()
	handler.ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Errorf("expected status %d, got %d", http.StatusOK, rec.Code)
	}
	if gotUserID != "user-ctx" {
		t.Errorf("expected UserID %q from context, got %q", "user-ctx", gotUserID)
	}
}
