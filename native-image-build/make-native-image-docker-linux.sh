#!/usr/bin/env bash
project_root_dir=${BASH_SOURCE%/*}/..
native_image_build_dir=${BASH_SOURCE%/*}/
tmp_build_dir=$native_image_build_dir/native-build

mkdir -p $tmp_build_dir
cp $project_root_dir/build/fatjar/*.jar $tmp_build_dir
${project_root_dir}/gradlew build
docker build -f $native_image_build_dir/Dockerfile -t kraal-example $tmp_build_dir
