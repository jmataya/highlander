SUBDIRS := content
BUILDDIRS := $(SUBDIRS:%=build-%)

clean: 
	rm content/*.html
	rm content/**/*.html

prep:
	npm install
	cd node_modules/aglio-theme-fox && npm install

build: prep $(BUILDDIRS)

$(BUILDDIRS):
	$(MAKE) -C $(@:build-%=%) build

test:
	true

docker: 
	true

docker-push:
	true

publish: build
	firebase use production
	firebase deploy

.PHONY: prep clean build test docker docker-push publish