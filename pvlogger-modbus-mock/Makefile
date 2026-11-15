BIN_PATH := ./build

.PHONY: build clean lint test

test:
	@go test ./...

# The mock server and backfill commands come next (backlog 23/30); until then
# there is nothing to build.
build: test

clean:
	@go clean
	@rm -rf ${BIN_PATH}

lint:
	@golangci-lint run
