#!/bin/bash

export MESOS_WEBUI_DIR=/usr/local/share/mesos/webui
WORK_DIR=/var/lib/mesos
IP=`hostname -I | awk '{print $1}'`
QUORUM={{mesos_quorum}}

/usr/local/sbin/mesos-master --hostname=$IP --quorum=$QUORUM --ip=$IP --work_dir=$WORK_DIR --zk={{zookeepers}}/mesos --cluster=fox