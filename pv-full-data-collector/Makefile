BINARY_NAME=full-data-collector
BIN_PATH := ./build
CMD_PATH := ./cmd/collector
LINUX_BUILD_OPTIONS := CGO_ENABLED=0 GOARCH=arm64 GOOS=linux
MACOS_BUILD_OPTIONS := CGO_ENABLED=0 GOARCH=arm64 GOOS=darwin

.PHONY: build clean lint prod test

test:
	@go test ./...

build: test
	@$(MACOS_BUILD_OPTIONS) go build -o ${BIN_PATH}/${BINARY_NAME}-darwin $(CMD_PATH)
	@$(LINUX_BUILD_OPTIONS) go build -o ${BIN_PATH}/${BINARY_NAME} $(CMD_PATH)

prod: test
	@go mod download
	@go mod verify
	@$(LINUX_BUILD_OPTIONS) go build -o /${BINARY_NAME} $(CMD_PATH)

clean:
	@go clean
	@rm -rf ${BIN_PATH}

lint:
	@golangci-lint run
