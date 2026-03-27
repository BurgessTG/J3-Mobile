package devtoken

import (
	"testing"

	"github.com/BurgessTG/J3-Mobile/server/internal/auth"
)

func TestGenerateProducesParsableToken(t *testing.T) {
	token, err := Generate("test-secret", "dev-user")
	if err != nil {
		t.Fatalf("Generate failed: %v", err)
	}

	claims, err := auth.ParseToken("test-secret", token)
	if err != nil {
		t.Fatalf("ParseToken failed: %v", err)
	}
	if claims.UserID != "dev-user" {
		t.Fatalf("UserID = %q, want %q", claims.UserID, "dev-user")
	}
}

func TestGenerateRejectsMissingInputs(t *testing.T) {
	if _, err := Generate("", "dev-user"); err == nil {
		t.Fatal("expected error for missing secret")
	}
	if _, err := Generate("test-secret", ""); err == nil {
		t.Fatal("expected error for missing user id")
	}
}
