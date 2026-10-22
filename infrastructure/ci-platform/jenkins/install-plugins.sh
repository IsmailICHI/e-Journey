#!/bin/sh
set -eu

exec jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt
