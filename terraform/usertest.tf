variable "ssh_user" {} 
variable "ssh_private_key" {} 

module "usertest1" {
    source = "./twostack"
    prefix = "usertest1"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}

module "usertest2" {
    source = "./twostack"
    prefix = "usertest2"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}
