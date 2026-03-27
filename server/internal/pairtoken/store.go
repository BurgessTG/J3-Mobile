package pairtoken

import (
	"crypto/rand"
	"crypto/sha256"
	"crypto/subtle"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"time"
)

var (
	ErrInvalidToken       = errors.New("invalid pairing token")
	ErrCredentialNotFound = errors.New("credential not found")
)

type Credential struct {
	ID         string     `json:"credentialId"`
	DeviceName string     `json:"deviceName"`
	TokenHash  string     `json:"tokenHash"`
	CreatedAt  time.Time  `json:"createdAt"`
	LastUsedAt *time.Time `json:"lastUsedAt,omitempty"`
	RevokedAt  *time.Time `json:"revokedAt,omitempty"`
}

func (c Credential) IsRevoked() bool {
	return c.RevokedAt != nil
}

type fileState struct {
	Credentials []Credential `json:"credentials"`
}

type Store struct {
	path string
	now  func() time.Time
}

func DefaultPath() (string, error) {
	configDir, err := os.UserConfigDir()
	if err != nil {
		return "", fmt.Errorf("resolve user config dir: %w", err)
	}
	return filepath.Join(configDir, "j3-mobile", "tokens.json"), nil
}

func NewStore(path string) *Store {
	return &Store{
		path: path,
		now:  time.Now,
	}
}

func (s *Store) Path() string {
	return s.path
}

func (s *Store) Create(deviceName string) (Credential, string, error) {
	deviceName = strings.TrimSpace(deviceName)
	if deviceName == "" {
		return Credential{}, "", fmt.Errorf("device name is required")
	}

	state, err := s.load()
	if err != nil {
		return Credential{}, "", err
	}

	token, err := generatePairingToken()
	if err != nil {
		return Credential{}, "", err
	}

	now := s.now().UTC()
	credential := Credential{
		ID:         generateCredentialID(),
		DeviceName: deviceName,
		TokenHash:  hashToken(token),
		CreatedAt:  now,
	}

	state.Credentials = append(state.Credentials, credential)
	if err := s.save(state); err != nil {
		return Credential{}, "", err
	}

	return credential, token, nil
}

func (s *Store) Authenticate(token string) (*Credential, error) {
	token = strings.TrimSpace(token)
	if token == "" {
		return nil, ErrInvalidToken
	}

	state, err := s.load()
	if err != nil {
		return nil, err
	}

	hash := hashToken(token)
	now := s.now().UTC()

	for i := range state.Credentials {
		credential := &state.Credentials[i]
		if credential.IsRevoked() || subtle.ConstantTimeCompare([]byte(credential.TokenHash), []byte(hash)) != 1 {
			continue
		}

		credential.LastUsedAt = &now
		if err := s.save(state); err != nil {
			return nil, err
		}

		copyValue := *credential
		return &copyValue, nil
	}

	return nil, ErrInvalidToken
}

func (s *Store) List() ([]Credential, error) {
	state, err := s.load()
	if err != nil {
		return nil, err
	}

	credentials := make([]Credential, len(state.Credentials))
	copy(credentials, state.Credentials)
	return credentials, nil
}

func (s *Store) Revoke(id string) error {
	id = strings.TrimSpace(id)
	if id == "" {
		return fmt.Errorf("credential id is required")
	}

	state, err := s.load()
	if err != nil {
		return err
	}

	now := s.now().UTC()
	for i := range state.Credentials {
		if state.Credentials[i].ID != id {
			continue
		}
		state.Credentials[i].RevokedAt = &now
		return s.save(state)
	}

	return ErrCredentialNotFound
}

func (s *Store) load() (*fileState, error) {
	if s.path == "" {
		return nil, fmt.Errorf("pair token store path is required")
	}

	raw, err := os.ReadFile(s.path)
	if errors.Is(err, os.ErrNotExist) {
		return &fileState{}, nil
	}
	if err != nil {
		return nil, fmt.Errorf("read token store: %w", err)
	}

	if len(raw) == 0 {
		return &fileState{}, nil
	}

	var state fileState
	if err := json.Unmarshal(raw, &state); err != nil {
		return nil, fmt.Errorf("decode token store: %w", err)
	}

	return &state, nil
}

func (s *Store) save(state *fileState) error {
	if err := os.MkdirAll(filepath.Dir(s.path), 0o700); err != nil {
		return fmt.Errorf("create token store dir: %w", err)
	}

	raw, err := json.MarshalIndent(state, "", "  ")
	if err != nil {
		return fmt.Errorf("encode token store: %w", err)
	}
	raw = append(raw, '\n')

	tempFile, err := os.CreateTemp(filepath.Dir(s.path), "tokens-*.json")
	if err != nil {
		return fmt.Errorf("create temp token store: %w", err)
	}

	tempPath := tempFile.Name()
	defer os.Remove(tempPath)

	if _, err := tempFile.Write(raw); err != nil {
		tempFile.Close()
		return fmt.Errorf("write temp token store: %w", err)
	}
	if err := tempFile.Chmod(0o600); err != nil {
		tempFile.Close()
		return fmt.Errorf("chmod temp token store: %w", err)
	}
	if err := tempFile.Close(); err != nil {
		return fmt.Errorf("close temp token store: %w", err)
	}

	if err := os.Rename(tempPath, s.path); err != nil {
		return fmt.Errorf("replace token store: %w", err)
	}

	return nil
}

func hashToken(token string) string {
	sum := sha256.Sum256([]byte(token))
	return hex.EncodeToString(sum[:])
}

func generateCredentialID() string {
	return "cred_" + randomHex(8)
}

func generatePairingToken() (string, error) {
	random := make([]byte, 32)
	if _, err := rand.Read(random); err != nil {
		return "", fmt.Errorf("generate pairing token: %w", err)
	}
	return "j3pt_" + base64.RawURLEncoding.EncodeToString(random), nil
}

func randomHex(size int) string {
	bytes := make([]byte, size)
	if _, err := rand.Read(bytes); err != nil {
		panic(fmt.Sprintf("generate random hex: %v", err))
	}
	return hex.EncodeToString(bytes)
}
