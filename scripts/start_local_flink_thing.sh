#! /usr/bin/bash

../flink-1.3.2/bin/taskmanager.sh stop
../flink-1.3.2/bin/taskmanager.sh stop
../flink-1.3.2/bin/taskmanager.sh stop
../flink-1.3.2/bin/taskmanager.sh stop
../flink-1.3.2/bin/stop-local.sh  

echo "deamons stopped"
sleep 5
echo "starting up again"


../flink-1.3.2/bin/start-local.sh  
../flink-1.3.2/bin/taskmanager.sh start
../flink-1.3.2/bin/taskmanager.sh start
../flink-1.3.2/bin/taskmanager.sh start
../flink-1.3.2/bin/taskmanager.sh start
# ../flink-1.3.2/bin/flink run build/libs/jayct-0.2.0-SNAPSHOT-all.jar build/gamma_images.json.gz build/clf.json build/rgr.json
