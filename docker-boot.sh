#!/bin/bash
GIT_VER=$(curl -s 'https://api.github.com/repos/cjww-development/gatekeeper/releases/latest' | jq -r '.tag_name')

echo "VERSION=$GIT_VER" >| './.env'

sbt -Dversion=$GIT_VER universal:packageZipTarball

docker build . -t cjww-development/gatekeeper:latest --build-arg VERSION=$GIT_VER

docker-compose up -d