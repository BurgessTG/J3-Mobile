package main

import (
	"flag"
	"fmt"
	"os"

	"github.com/BurgessTG/J3-Mobile/server/internal/devtoken"
	"github.com/joho/godotenv"
)

func main() {
	_ = godotenv.Load()

	userID := flag.String("user-id", "", "user id to embed in the generated JWT (migration-only)")
	flag.Parse()

	token, err := devtoken.Generate(os.Getenv("JWT_SECRET"), *userID)
	if err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}

	fmt.Println(token)
}
