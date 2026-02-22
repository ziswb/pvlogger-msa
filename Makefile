SUB_DIRS := pv-data-collector pv-dynamic-data-collector pvlogger-fusionsolar pvlogger-kpi-exporter pvlogger-modbus-data-collector

.PHONY: build clean lint $(SUB_DIRS)

build clean lint: $(SUB_DIRS)

$(SUB_DIRS):
	$(MAKE) -C $@ $(MAKECMDGOALS)
