.PHONY: server-build server-test server-lint server-fmt server-launchd server-pair-token mobile-build mobile-ios-open mobile-ios-archive mobile-ios-testflight check

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
	cd mobile && if [ -z "$$JAVA_HOME" ] && [ -x /usr/libexec/java_home ]; then export JAVA_HOME="$$(/usr/libexec/java_home -v 21 2>/dev/null || /usr/libexec/java_home 2>/dev/null)"; fi && ./gradlew composeApp:compileDebugKotlinAndroid

mobile-ios-open:
	open mobile/iosApp/iosApp.xcodeproj

mobile-ios-archive:
	cd mobile && ./scripts/ios/archive.sh

mobile-ios-testflight:
	cd mobile && ./scripts/ios/upload_testflight.sh

# All
check: server-fmt server-lint server-test server-build
