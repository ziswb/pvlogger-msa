BIN_PATH := ./build
# Target architecture of the linux binary: the host's by default; the image
# build passes the platform's (make prod GOARCH=$(TARGETARCH), backlog 56).
HOSTARCH := $(shell env -u GOARCH go env GOARCH)
GOARCH ?= $(HOSTARCH)
LINUX_BUILD_OPTIONS := CGO_ENABLED=0 GOARCH=$(GOARCH) GOOS=linux
MACOS_BUILD_OPTIONS := CGO_ENABLED=0 GOARCH=$(HOSTARCH) GOOS=darwin
COMMANDS := mock-server backfill

.PHONY: build clean lint prod test

# Tests run for the build host: a GOARCH from the command line is exported to
# the recipes, and foreign-arch test binaries cannot run.
test:
	@env -u GOARCH go test ./...

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
