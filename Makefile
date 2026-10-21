SUB_DIRS := pv-fast-data-collector pv-full-data-collector pvlogger-fusionsolar pvlogger-fusionsolar-auth pvlogger-kpi-exporter pv-device-data-collector pvlogger-battery-simulator
# Go libraries: no Makefile of their own, linted from here.
LIB_DIRS := pv-logger pvlogger-shared-modbus

.PHONY: build clean lint test lint-libs $(SUB_DIRS)

build clean lint test: $(SUB_DIRS)

lint: lint-libs

lint-libs:
	@for d in $(LIB_DIRS); do (cd $$d && golangci-lint run ./...) || exit 1; done

$(SUB_DIRS):
	$(MAKE) -C $@ $(MAKECMDGOALS)
