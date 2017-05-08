# Development environment Makefile
include makelib
-include goldrush.cfg
header = $(call baseheader, $(1), root)

prepare:
	sudo pip install -r tabernacle/requirements.txt

config:
	cd tabernacle && ansible-playbook --inventory-file=inventory/static/dev ansible/goldrush_config_gen.yml

up:
	$(call header, Creating GCE Machine)
	ansible-playbook --user=$(GOOGLE_SSH_USERNAME) --private-key=$(GOOGLE_SSH_KEY) --extra-vars '{"FIRST_RUN": true}' tabernacle/ansible/goldrush_appliance.yml
	@cat goldrush.log

debug:
	ansible-playbook --user=$(GOOGLE_SSH_USERNAME) --private-key=$(GOOGLE_SSH_KEY) tabernacle/ansible/goldrush_debug.yml

destroy:
	$(call header, Destroying GCE Machine)
	ansible-playbook tabernacle/ansible/goldrush_appliance_destroy.yml
	@rm -rf goldrush.log
	@rm -rf goldrush.state

update-app:
	cd tabernacle && ansible-playbook -v -i inventory/static/dev ansible/goldrush_update_app.yml

provision:
	$(call header, Provisioning GCE Machine)
	ansible-playbook --user=$(GOOGLE_SSH_USERNAME) --private-key=$(GOOGLE_SSH_KEY) tabernacle/ansible/goldrush_appliance.yml
	@cat goldrush.log

.PHONY: prepare config up debug destroy update-app provision
