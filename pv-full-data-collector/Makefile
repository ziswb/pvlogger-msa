BINARY_NAME=full-data-collector
BIN_PATH := ./build
CMD_PATH := ./cmd/collector
# Target architecture of the linux binary: the host's by default; the image
# build passes the platform's (make prod GOARCH=$(TARGETARCH), backlog 56).
HOSTARCH := $(shell env -u GOARCH go env GOARCH)
GOARCH ?= $(HOSTARCH)
LINUX_BUILD_OPTIONS := CGO_ENABLED=0 GOARCH=$(GOARCH) GOOS=linux
MACOS_BUILD_OPTIONS := CGO_ENABLED=0 GOARCH=$(HOSTARCH) GOOS=darwin

.PHONY: build clean lint prod test

# Tests run for the build host: a GOARCH from the command line is exported to
# the recipes, and foreign-arch test binaries cannot run.
test:
	@env -u GOARCH go test ./...

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
