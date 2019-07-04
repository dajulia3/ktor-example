#!/usr/bin/env bash

project_root_dir=${BASH_SOURCE%/*}/../..
current_dir=${BASH_SOURCE%/*}/

${project_root_dir}/gradlew test -PgenerateGraalConfig