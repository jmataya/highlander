variable "ssh_user" {}

variable "ssh_private_key" {}

variable "account_file" {}

variable "gce_project" {}

variable "region" {}

variable "appliance_image" {}

variable "consul_leader" {}

variable "dnsimple_token" {}

variable "dnsimple_account" {}

provider "google" {
  credentials = "${file(var.account_file)}"
  project     = "${var.gce_project}"
  region      = "${var.region}"
}

##############################################
# Setup Amazon Feature Branch
##############################################
module "amazon" {
  source           = "../../modules/gce/appliance"
  instance_name    = "feature-branch-amazon"
  dns_record       = "feature-branch-amazon"
  appliance_image  = "${var.appliance_image}"
  consul_leader    = "${var.consul_leader}"
  ssh_user         = "${var.ssh_user}"
  ssh_private_key  = "${var.ssh_private_key}"
  dnsimple_account = "${var.dnsimple_account}"
  dnsimple_token   = "${var.dnsimple_token}"
}

##############################################
# Setup Bigbag Feature Branch
##############################################
module "bigbag" {
  source           = "../../modules/gce/appliance"
  instance_name    = "feature-branch-bigbag"
  dns_record       = "feature-branch-bigbag"
  appliance_image  = "${var.appliance_image}"
  consul_leader    = "${var.consul_leader}"
  ssh_user         = "${var.ssh_user}"
  ssh_private_key  = "${var.ssh_private_key}"
  dnsimple_account = "${var.dnsimple_account}"
  dnsimple_token   = "${var.dnsimple_token}"
}

##############################################
# Setup Apple Pay Feature Branch
##############################################
module "applepay" {
  source           = "../../modules/gce/appliance"
  instance_name    = "feature-branch-apple"
  dns_record       = "feature-branch-apple"
  appliance_image  = "${var.appliance_image}"
  consul_leader    = "${var.consul_leader}"
  ssh_user         = "${var.ssh_user}"
  ssh_private_key  = "${var.ssh_private_key}"
  dnsimple_account = "${var.dnsimple_account}"
  dnsimple_token   = "${var.dnsimple_token}"
}

##############################################
# Setup Tumi2 Feature Branch
##############################################
module "tumi2" {
  source           = "../../modules/gce/appliance"
  instance_name    = "tumi2"
  dns_record       = "tumi2"
  appliance_image  = "${var.appliance_image}"
  consul_leader    = "${var.consul_leader}"
  ssh_user         = "${var.ssh_user}"
  ssh_private_key  = "${var.ssh_private_key}"
  dnsimple_account = "${var.dnsimple_account}"
  dnsimple_token   = "${var.dnsimple_token}"
}
