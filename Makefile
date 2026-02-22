SUB_DIRS := pv-fast-data-collector pv-full-data-collector pvlogger-fusionsolar pvlogger-kpi-exporter pv-device-data-collector

.PHONY: build clean lint $(SUB_DIRS)

build clean lint: $(SUB_DIRS)

$(SUB_DIRS):
	$(MAKE) -C $@ $(MAKECMDGOALS)
