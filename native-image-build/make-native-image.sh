#!/usr/bin/env bash
project_root_dir=${BASH_SOURCE%/*}/..

${project_root_dir}/gradlew build
docker build -f Dockerfile -t kraal-example ${project_root_dir}/build/fatjar


