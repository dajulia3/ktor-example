#!/usr/bin/env bash
project_root_dir=${BASH_SOURCE%/*}/..
current_dir=${BASH_SOURCE%/*}/
tmp_build_dir=${current_dir}/native-build


mkdir -p ${tmp_build_dir}
cp ${project_root_dir}/build/fatjar/*.jar ${tmp_build_dir}
${project_root_dir}/gradlew build
docker build -f ${current_dir}/Dockerfile -t ktor-application ${tmp_build_dir}
