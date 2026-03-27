.PHONY: server-build server-test server-lint server-fmt server-launchd server-pair-token mobile-build check

# Go server
server-build:
	cd server && go build ./...

server-test:
	cd server && go test ./... -v -race

server-lint:
	cd server && golangci-lint run

server-fmt:
	cd server && gofmt -l -w .

server-launchd:
	cd server && ./scripts/install-launchd.sh

server-pair-token:
	cd server && go run ./cmd/pair-token list

# KMP mobile (commonMain + androidMain on Linux)
mobile-build:
	cd mobile && ./gradlew composeApp:compileDebugKotlinAndroid

# All
check: server-fmt server-lint server-test server-build
