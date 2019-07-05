#!/usr/bin/env bash
project_root_dir=${BASH_SOURCE%/*}/../../
current_dir=${BASH_SOURCE%/*}

$project_root_dir/gradlew build -PgenerateGraalConfig

native-image \
    --report-unsupported-elements-at-runtime \
    -H:+ReportExceptionStackTraces \
    -H:Log=registerResource \
    -H:ReflectionConfigurationFiles=${current_dir}/generated-config/reflect-config.json,${current_dir}/manual-config/reflect-config.json\
    -H:ResourceConfigurationFiles=${current_dir}/generated-config/resource-config.json,$current_dir/manual-config/resource-config.json \
    --initialize-at-build-time \
    --no-fallback \
    --no-server \
    -jar ${project_root_dir}/build/fatjar/application.jar


#run it:
# example --enable-url-protocols=http --no-server

