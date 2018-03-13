#!/usr/bin/env bash

git config --global user.name "$GIT_NAME"
git config --global user.email "$GIT_EMAIL"
git config --global credential.helper "store --file=~/.git-credentials"
echo "https://$GH_TOKEN:@github.com" > ~/.git-credentials

git clone https://${GH_TOKEN}@github.com/${TRAVIS_REPO_SLUG}.git -b gh-pages gh-pages --single-branch > /dev/null
cd gh-pages

mkdir -p report
cp -r ../build/reports/tests/test/* ./report/

git add -A ./report/*

git commit -a -m "Updating reports for Travis build: https://travis-ci.org/$TRAVIS_REPO_SLUG/builds/$TRAVIS_BUILD_ID"
git push origin HEAD
cd ..
rm -rf gh-pages
