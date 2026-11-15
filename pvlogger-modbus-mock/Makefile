BIN_PATH := ./build
LINUX_BUILD_OPTIONS := CGO_ENABLED=0 GOARCH=arm64 GOOS=linux
MACOS_BUILD_OPTIONS := CGO_ENABLED=0 GOARCH=arm64 GOOS=darwin
COMMANDS := mock-server

.PHONY: build clean lint prod test

test:
	@go test ./...

build: test
	@for c in $(COMMANDS); do \
		$(MACOS_BUILD_OPTIONS) go build -o ${BIN_PATH}/$$c-darwin ./cmd/$$c && \
		$(LINUX_BUILD_OPTIONS) go build -o ${BIN_PATH}/$$c ./cmd/$$c || exit 1; \
	done

prod: test
	@go mod download
	@go mod verify
	@for c in $(COMMANDS); do $(LINUX_BUILD_OPTIONS) go build -o /$$c ./cmd/$$c || exit 1; done

clean:
	@go clean
	@rm -rf ${BIN_PATH}

lint:
	@golangci-lint run
