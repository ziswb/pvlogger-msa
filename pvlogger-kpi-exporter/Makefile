SUB_DIRS := app

.PHONY: build clean lint $(SUB_DIRS)

build clean lint: $(SUB_DIRS)

$(SUB_DIRS):
	$(MAKE) -C $@ $(MAKECMDGOALS)
