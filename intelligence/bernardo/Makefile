include ../../makelib
header = $(call baseheader, $(1), bernardo)

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_TAG ?= bernardo
DOCKER_BRANCH ?= master

build:
	$(call header, Building)
	mkdir -p build
	cd build && cmake .. && make -j4

test:
	$(call header, Testing)
	true

docker:
	$(call header, Dockerizing)
	docker build -t $(DOCKER_TAG) .

docker-push:
	$(call header, Registering)
	docker tag $(DOCKER_TAG) $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)
	docker push $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)

.PHONY: build test docker docker-push