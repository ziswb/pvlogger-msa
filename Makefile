SUB_DIRS := pvlogger-fusionsolar pvlogger-kpi-exporter pvlogger-modbus-data-collector

.PHONY: build clean lint $(SUB_DIRS)

build clean lint: $(SUB_DIRS)

$(SUB_DIRS):
	$(MAKE) -C $@ $(MAKECMDGOALS)
