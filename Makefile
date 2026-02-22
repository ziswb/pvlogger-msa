SUB_DIRS := pv-quick-data-collector pv-full-data-collector pvlogger-fusionsolar pvlogger-kpi-exporter pvlogger-modbus-data-collector

.PHONY: build clean lint $(SUB_DIRS)

build clean lint: $(SUB_DIRS)

$(SUB_DIRS):
	$(MAKE) -C $@ $(MAKECMDGOALS)
