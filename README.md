# prov-shit

### Dependencies

- [Ansible](http://docs.ansible.com/ansible/intro_installation.html#installation) (1.9.x)
- [ansible-lint](https://github.com/willthames/ansible-lint#setup) (2.2.x)
- [Go](https://golang.org/doc/install) (1.5)

### Setup

- Checkout [phoenix-scala](https://github.com/FoxComm/phoenix-scala)
- Checkout [green-river](https://github.com/FoxComm/green-river)
- Build all projects:

    ```
    $ cd phoenix-scala
    $ sbt assembly
    $ cd ../green-river
    $ sbt assembly
    $ cd ../prov-shit
    $ make build
    ```

### Lint ansible scripts

    $ make lint

### Vagrant

#### First of all you need to create `.vault_pass` file with vault password

#### If you want to run an appliance with the backend and ashes all on one machine.

    $ vagrant up

This will bring up a machine on ip 192.168.10.111 with everything installed.

#### If you want to run just the backend

    $ vagrant up backend

This will bring up a machine on 192.168.10.111 with just the db, phoenix and green river installed.


#### If you  want to run just ashes

    $ vagrant up ashes

This will bring up a machine on 192.168.10.112 with ashes installed.
By default it looks for the backed at '192.168.10.111'. If you want to
change this you can set the environtment variable 'BACKEND_HOST'.

export BACKEND_HOST=10.240.0.8

#### If you want to run in GCE

Install the GCE vagrant provider

    $ vagrant plugin install vagrant-google

Add the following vagrant box.

    $ vagrant box add gce https://github.com/mitchellh/vagrant-google/raw/master/google.box

Set the following environment variables.

    $ export GOOGLE_SSH_USERNAME=ubuntu
    $ export GOOGLE_SSH_KEY=~/.ssh/google_compute_engine # Or the location of your key
    $ export GOOGLE_CLIENT_EMAIL=<Your FoxCommerce email>

Download a JSON key for our GCE environment. You can follow 
[Google's instructions for generating a private key](https://cloud.google.com/storage/docs/authentication#generating-a-private-key).

Make sure to generate a JSON key. And save it with the name `foxcomm-staging.json` in the root directory of this project.

Once downloaded, set the location.

    $ export GOOGLE_JSON_KEY_LOCATION=`pwd`/foxcomm-staging.json

Then run

    $ vagrant up --provider=google

Test machines are created without a public facing IP address, so you'll need to use the VPN to access it.
    
### Update Green River assembly

You can pass green river `*.jar` file to VM without re-provisioning.

Just run:

    $ tools/update_greenriver.sh

or:

    $ MACHINE=backend PROVIDER=vmware_fusion tools/update_greenriver.sh    
