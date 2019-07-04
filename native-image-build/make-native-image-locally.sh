#!/usr/bin/env bash
project_root_dir=${BASH_SOURCE%/*}/..
native_image_build_dir=${BASH_SOURCE%/*}

$project_root_dir/gradlew build -PgenerateGraalConfig
#    --static \

echo "config files>>> ${native_image_build_dir}/base-reflect.json"

native-image \
    --report-unsupported-elements-at-runtime \
    -H:+ReportExceptionStackTraces \
    -H:Log=registerResource \
    -H:ReflectionConfigurationFiles=${native_image_build_dir}/generated-config/reflect-config.json,${native_image_build_dir}/manual-config/reflect-config.json\
    -H:ResourceConfigurationFiles=${native_image_build_dir}/generated-config/resource-config.json,$native_image_build_dir/manual-config/resource-config.json \
    --initialize-at-build-time \
    --no-fallback \
    --no-server \
    -jar ${project_root_dir}/build/fatjar/application.jar


#run it:
# example --enable-url-protocols=http --no-server

