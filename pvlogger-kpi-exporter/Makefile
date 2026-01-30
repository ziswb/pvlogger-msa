BINARY_NAME=exporter
BIN_PATH := ./build
CMD_PATH := ./cmd/exporter
BUILD_OPTIONS := CGO_ENABLED=0 GOARCH=arm64 GOOS=darwin

.PHONY: build clean lint prod

build:
	@$(BUILD_OPTIONS) go build -o ${BIN_PATH}/${BINARY_NAME}-darwin $(CMD_PATH)
	@$(BUILD_OPTIONS) go build -o ${BIN_PATH}/${BINARY_NAME} $(CMD_PATH)

prod:
	@go mod download
	@go mod verify
	@$(BUILD_OPTIONS) go build -o /${BINARY_NAME} $(CMD_PATH)

clean:
	@go clean
	@rm -rf ${BIN_PATH}

lint:
	@golangci-lint run