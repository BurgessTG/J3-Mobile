.PHONY: server-build server-test server-lint server-fmt mobile-build check

# Go server
server-build:
	cd server && go build ./...

server-test:
	cd server && go test ./... -v -race

server-lint:
	cd server && golangci-lint run

server-fmt:
	cd server && gofmt -l -w .

# KMP mobile (commonMain + androidMain on Linux)
mobile-build:
	cd mobile && ./gradlew composeApp:compileKotlinAndroid

# All
check: server-fmt server-lint server-test server-build
