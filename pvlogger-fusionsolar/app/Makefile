BINARY_NAME := fusionsolar
BIN_PATH := ../build
BUILD_OPTIONS := CGO_ENABLED=0 GOARCH=arm64 GOOS=darwin

.PHONY: build clean lint prod

build:
	$(BUILD_OPTIONS) go build -o ${BIN_PATH}/${BINARY_NAME}-darwin
	$(BUILD_OPTIONS) go build -o ${BIN_PATH}/${BINARY_NAME}

prod:
	go mod download
	go mod verify
	$(BUILD_OPTIONS) go build -o /${BINARY_NAME}

clean:
	go clean
	rm -rf ${BIN_PATH}

lint:
	golangci-lint run