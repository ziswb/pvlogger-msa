BINARY_NAME=exporter
BIN_PATH := ./build
CMD_PATH := ./cmd/exporter
# Target architecture of the linux binary: the host's by default; the image
# build passes the platform's (make prod GOARCH=$(TARGETARCH), backlog 56).
HOSTARCH := $(shell env -u GOARCH go env GOARCH)
GOARCH ?= $(HOSTARCH)
LINUX_BUILD_OPTIONS := CGO_ENABLED=0 GOARCH=$(GOARCH) GOOS=linux
MACOS_BUILD_OPTIONS := CGO_ENABLED=0 GOARCH=$(HOSTARCH) GOOS=darwin

.PHONY: build clean lint prod test relay-bundle

# Relay bundle for the receiver's server: binary, unit, env template,
# install.sh, checksums. make relay-bundle VERSION=3.5.0 [RELAY_ARCH=amd64]
RELAY_ARCH ?= amd64
RELAY_BUNDLE = pvlogger-relay-$(VERSION)-$(RELAY_ARCH)
relay-bundle: test
	@test -n "$(VERSION)" || { echo "VERSION=x.y.z required"; exit 1; }
	@rm -rf $(BIN_PATH)/$(RELAY_BUNDLE) && mkdir -p $(BIN_PATH)/$(RELAY_BUNDLE)
	@CGO_ENABLED=0 GOOS=linux GOARCH=$(RELAY_ARCH) go build -trimpath -o $(BIN_PATH)/$(RELAY_BUNDLE)/pvlogger-kpi-exporter $(CMD_PATH)
	@cp systemd/pvlogger-relay.service systemd/install.sh $(BIN_PATH)/$(RELAY_BUNDLE)/
	@cp systemd/relay.env.example $(BIN_PATH)/$(RELAY_BUNDLE)/relay.env
	@echo "$(VERSION) $(RELAY_ARCH)" > $(BIN_PATH)/$(RELAY_BUNDLE)/BUNDLE
	@cd $(BIN_PATH)/$(RELAY_BUNDLE) && shasum -a 256 pvlogger-kpi-exporter pvlogger-relay.service install.sh BUNDLE > SHA256SUMS
	@cd $(BIN_PATH) && COPYFILE_DISABLE=1 tar -czf $(RELAY_BUNDLE).tar.gz $(RELAY_BUNDLE)
	@shasum -a 256 $(BIN_PATH)/$(RELAY_BUNDLE).tar.gz

# Tests run on the build host: `make prod GOARCH=arm64` exports GOARCH to the
# recipes, and arm64 test binaries cannot run on an amd64 builder (CI images.yml).
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
