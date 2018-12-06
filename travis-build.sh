#!/bin/bash
set -e
chmod +x ./travis/*.sh

./gradlew test assemble --no-daemon

filename=$(find build/libs -name "*.jar" | head -1)
filename=$(basename "$filename")

EXIT_STATUS=0
echo "Publishing archives for branch $TRAVIS_BRANCH"
if [[ -n $TRAVIS_TAG ]] || [[ $TRAVIS_BRANCH == 'master' && $TRAVIS_PULL_REQUEST == 'false' ]]; then

  echo "Publishing archives"

  if [[ -n $TRAVIS_TAG ]]; then
      ./gradlew bintrayUpload --no-daemon  || EXIT_STATUS=$?
  else
      ./gradlew publish --no-daemon || EXIT_STATUS=$?
  fi

  ./gradlew docs || EXIT_STATUS=$?
  ./travis/publish_docs.sh
fi

exit $EXIT_STATUS