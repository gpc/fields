#!/bin/bash
function load_gvm {
	if [ ! -f ~/.gvm/etc/config ]; then
		# ~/.gvm is some golang related tool in Travis CI, just remove it and install gvm
		rm -rf ~/.gvm
		curl -s get.gvmtool.net | bash
		perl -i -p -e 's/gvm_auto_answer=false/gvm_auto_answer=true/' ~/.gvm/etc/config
	fi

	source ~/.gvm/bin/gvm-init.sh
}

function install_and_use_grails {
	grails_version=$1
	load_gvm
	gvm install grails $grails_version
	if [ $? -ne 0 ]; then
		# grails version not available in gvm yet, download directly from s3
		set -e
		(
		set -e
		cd /tmp
		curl -O http://dist.springframework.org.s3.amazonaws.com/release/GRAILS/grails-${grails_version}.zip
		unzip grails-${grails_version}.zip -d ~/.gvm/grails/ 
		mv ~/.gvm/grails/{grails-${grails_version},${grails_version}}
		rm grails-${grails_version}.zip
		)	
	fi
	gvm default grails $grails_version
	gvm use grails $grails_version
}

use_grails_version="${GRAILS_VERSION:-2.4.4}"
install_and_use_grails $use_grails_version
perl -i -p -e "s/app\\.grails\\.version=.*/app.grails.version=$use_grails_version/" application.properties


set -e
grails refresh-dependencies --non-interactive
grails test-app --non-interactive unit:
grails test-app --non-interactive -echoOut -echoErr :cli
grails package-plugin --non-interactive
grails maven-install --non-interactive

exit 0
