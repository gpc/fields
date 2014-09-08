#!/bin/bash
set -e
./grailsw refresh-dependencies --non-interactive

# Grails wrapper doesn't add executable permission to grails script
chmod a+x ~/.grails/wrapper/*/grails-*/bin/grails || true

./grailsw test-app --non-interactive
./grailsw package-plugin --non-interactive
./grailsw maven-install --non-interactive
