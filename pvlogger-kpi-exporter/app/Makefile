BINARY_NAME=exporter
BUILD_PATH=../build

.PHONY: build clean lint

build:
	GOARCH=arm64 GOOS=darwin go build -o ${BUILD_PATH}/${BINARY_NAME}-darwin
	GOARCH=arm64 GOOS=linux go build -o ${BUILD_PATH}/${BINARY_NAME}

clean:
	go clean
	rm -rf ${BUILD_PATH}

lint:
	golangci-lint run