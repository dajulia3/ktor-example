#!/usr/bin/env bash

current_dir=${BASH_SOURCE%/*}/
project_root_dir=${current_dir}/../../
tmp_build_dir=${current_dir}/../native-build
scripts_dir=${current_dir}/supporting-scripts

function killBackgroundApplication {
  kill $!
}

./$scripts_dir/make-native-image-docker-linux
./$scripts_dir/run-native-image&

${project_root_dir}/gradlew

trap killBackgroundApplication EXIT





