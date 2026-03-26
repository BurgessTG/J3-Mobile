package pairtoken

import (
	"errors"
	"path/filepath"
	"testing"
	"time"
)

func newTestStore(t *testing.T) *Store {
	t.Helper()

	store := NewStore(filepath.Join(t.TempDir(), "tokens.json"))
	now := time.Date(2026, 3, 25, 12, 0, 0, 0, time.UTC)
	store.now = func() time.Time { return now }
	return store
}

func TestCreateAndAuthenticate(t *testing.T) {
	store := newTestStore(t)

	credential, token, err := store.Create("Jacob iPhone")
	if err != nil {
		t.Fatalf("Create failed: %v", err)
	}

	if credential.ID == "" {
		t.Fatal("expected credential ID to be populated")
	}
	if token == "" {
		t.Fatal("expected pairing token to be populated")
	}

	authenticated, err := store.Authenticate(token)
	if err != nil {
		t.Fatalf("Authenticate failed: %v", err)
	}

	if authenticated.ID != credential.ID {
		t.Fatalf("expected credential ID %q, got %q", credential.ID, authenticated.ID)
	}
	if authenticated.DeviceName != "Jacob iPhone" {
		t.Fatalf("expected device name to round-trip, got %q", authenticated.DeviceName)
	}
	if authenticated.LastUsedAt == nil {
		t.Fatal("expected last used at to be populated")
	}
}

func TestAuthenticateRejectsUnknownToken(t *testing.T) {
	store := newTestStore(t)

	if _, err := store.Authenticate("j3pt_invalid"); !errors.Is(err, ErrInvalidToken) {
		t.Fatalf("expected ErrInvalidToken, got %v", err)
	}
}

func TestRevokePreventsFutureAuthentication(t *testing.T) {
	store := newTestStore(t)

	credential, token, err := store.Create("Mac")
	if err != nil {
		t.Fatalf("Create failed: %v", err)
	}

	if err := store.Revoke(credential.ID); err != nil {
		t.Fatalf("Revoke failed: %v", err)
	}

	if _, err := store.Authenticate(token); !errors.Is(err, ErrInvalidToken) {
		t.Fatalf("expected ErrInvalidToken after revoke, got %v", err)
	}
}

func TestRevokeRejectsUnknownCredential(t *testing.T) {
	store := newTestStore(t)

	if err := store.Revoke("cred_missing"); !errors.Is(err, ErrCredentialNotFound) {
		t.Fatalf("expected ErrCredentialNotFound, got %v", err)
	}
}
