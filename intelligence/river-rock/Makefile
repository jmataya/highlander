include ../../makelib
header = $(call baseheader, $(1), river-rock)

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_TAG ?= river-rock
DOCKER_BRANCH ?= master

build:
	$(call header, Building)
	go build

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