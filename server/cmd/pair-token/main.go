package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"os"
	"text/tabwriter"

	"github.com/BurgessTG/J3-Mobile/server/internal/pairtoken"
	"github.com/joho/godotenv"
)

func main() {
	_ = godotenv.Load()

	if len(os.Args) < 2 {
		usage()
		os.Exit(1)
	}

	switch os.Args[1] {
	case "create":
		runCreate(os.Args[2:])
	case "list":
		runList(os.Args[2:])
	case "revoke":
		runRevoke(os.Args[2:])
	default:
		usage()
		os.Exit(1)
	}
}

func runCreate(args []string) {
	fs := flag.NewFlagSet("create", flag.ExitOnError)
	deviceName := fs.String("name", "", "human-readable device name")
	storePath := fs.String("store", "", "override pair token store path")
	fs.Parse(args)

	store, err := openStore(*storePath)
	if err != nil {
		fail(err)
	}

	credential, token, err := store.Create(*deviceName)
	if err != nil {
		fail(err)
	}

	fmt.Printf("Credential ID: %s\n", credential.ID)
	fmt.Printf("Device Name: %s\n", credential.DeviceName)
	fmt.Printf("Store: %s\n", store.Path())
	fmt.Printf("Pairing Token: %s\n", token)
}

func runList(args []string) {
	fs := flag.NewFlagSet("list", flag.ExitOnError)
	storePath := fs.String("store", "", "override pair token store path")
	asJSON := fs.Bool("json", false, "emit JSON")
	fs.Parse(args)

	store, err := openStore(*storePath)
	if err != nil {
		fail(err)
	}

	credentials, err := store.List()
	if err != nil {
		fail(err)
	}

	type row struct {
		ID         string  `json:"credentialId"`
		DeviceName string  `json:"deviceName"`
		CreatedAt  string  `json:"createdAt"`
		LastUsedAt *string `json:"lastUsedAt,omitempty"`
		RevokedAt  *string `json:"revokedAt,omitempty"`
	}

	rows := make([]row, 0, len(credentials))
	for _, credential := range credentials {
		item := row{
			ID:         credential.ID,
			DeviceName: credential.DeviceName,
			CreatedAt:  credential.CreatedAt.UTC().Format("2006-01-02T15:04:05Z07:00"),
		}
		if credential.LastUsedAt != nil {
			value := credential.LastUsedAt.UTC().Format("2006-01-02T15:04:05Z07:00")
			item.LastUsedAt = &value
		}
		if credential.RevokedAt != nil {
			value := credential.RevokedAt.UTC().Format("2006-01-02T15:04:05Z07:00")
			item.RevokedAt = &value
		}
		rows = append(rows, item)
	}

	if *asJSON {
		encoder := json.NewEncoder(os.Stdout)
		encoder.SetIndent("", "  ")
		_ = encoder.Encode(rows)
		return
	}

	w := tabwriter.NewWriter(os.Stdout, 0, 0, 2, ' ', 0)
	fmt.Fprintln(w, "CREDENTIAL ID\tDEVICE NAME\tCREATED AT\tLAST USED\tREVOKED")
	for _, item := range rows {
		lastUsed := ""
		if item.LastUsedAt != nil {
			lastUsed = *item.LastUsedAt
		}
		revoked := ""
		if item.RevokedAt != nil {
			revoked = *item.RevokedAt
		}
		fmt.Fprintf(w, "%s\t%s\t%s\t%s\t%s\n", item.ID, item.DeviceName, item.CreatedAt, lastUsed, revoked)
	}
	_ = w.Flush()
}

func runRevoke(args []string) {
	fs := flag.NewFlagSet("revoke", flag.ExitOnError)
	credentialID := fs.String("id", "", "credential ID to revoke")
	storePath := fs.String("store", "", "override pair token store path")
	fs.Parse(args)

	store, err := openStore(*storePath)
	if err != nil {
		fail(err)
	}

	if err := store.Revoke(*credentialID); err != nil {
		fail(err)
	}

	fmt.Printf("Revoked credential %s\n", *credentialID)
}

func openStore(override string) (*pairtoken.Store, error) {
	if override != "" {
		return pairtoken.NewStore(override), nil
	}

	path, err := pairtoken.DefaultPath()
	if err != nil {
		return nil, err
	}
	return pairtoken.NewStore(path), nil
}

func fail(err error) {
	fmt.Fprintln(os.Stderr, err)
	os.Exit(1)
}

func usage() {
	fmt.Fprintln(os.Stderr, "usage:")
	fmt.Fprintln(os.Stderr, "  go run ./cmd/pair-token create --name <device>")
	fmt.Fprintln(os.Stderr, "  go run ./cmd/pair-token list [--json]")
	fmt.Fprintln(os.Stderr, "  go run ./cmd/pair-token revoke --id <credential-id>")
}
