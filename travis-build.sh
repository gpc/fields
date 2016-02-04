#!/bin/bash
function load_sdkman {
	if [ ! -e ~/.sdkman ]; then
		curl -s http://get.sdkman.io | bash
	fi

    perl -i -p -e 's/sdkman_auto_answer=false/sdkman_auto_answer=true/' ~/.sdkman/etc/config
	export SDKMAN_DIR=~/.sdkman && source ~/.sdkman/bin/sdkman-init.sh
}

function install_and_use_grails {
	grails_version=$1
	load_sdkman
	sdk install grails $grails_version
	sdk default grails $grails_version
	sdk use grails $grails_version
}

use_grails_version="${GRAILS_VERSION:-2.4.4}"
install_and_use_grails $use_grails_version
perl -i -p -e "s/app\\.grails\\.version=.*/app.grails.version=$use_grails_version/" application.properties

set -e
grails refresh-dependencies --non-interactive
grails test-app --non-interactive unit:
grails test-app --non-interactive integration:
grails test-app --non-interactive -echoOut -echoErr :cli
grails package-plugin --non-interactive
grails maven-install --non-interactive

exit 0
