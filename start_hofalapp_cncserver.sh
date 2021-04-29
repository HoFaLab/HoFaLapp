#!/usr/bin/env bash

IP=$(ifconfig | grep -Eo 'inet (addr:)?([0-9]*\.){3}[0-9]*' | grep -Eo '([0-9]*\.){3}[0-9]*' | grep -v '127.0.0.1')

./hofalapp_cncserver.py $IP 1337 /dev/ttyUSB0
#./hofalapp_cncserver.py $IP 1337 none
