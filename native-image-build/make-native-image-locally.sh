#!/usr/bin/env bash
project_root_dir=${BASH_SOURCE%/*}/..
native_image_build_dir=${BASH_SOURCE%/*}/
#native-image  H:Log=registerResource --report-unsupported-elements-at-runtime --jar ${project_root_dir}/build/fatjar/example.jar

./gradlew build
#    --static \
native-image \
    --report-unsupported-elements-at-runtime \
    -H:Log=registerResource \
    -H:ReflectionConfigurationFiles=${native_image_build_dir}/reflect.json \
    -H:ResourceConfigurationFiles=${native_image_build_dir}/resource-config.json \
    --initialize-at-build-time \
    --no-fallback \
    --no-server \
    -jar ${project_root_dir}/build/fatjar/example.jar
#run it:
# example --enable-url-protocols=http --no-server