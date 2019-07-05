# Copyright 2018-2019 Hewlett Packard Enterprise Development LP
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
# documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
# rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
# permit persons to whom the Software is furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
# Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
# WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
# COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
# OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

# docker build -f Dockerfile -t ktor-application build/fatjar

##NOTE: we use initialize-at-buildtime to avoid errors when starting the server

FROM findepi/graalvm:19.0.2-native AS build
RUN ls
COPY native-build/application.jar .
COPY generated-config ./generated-config
COPY manual-config ./manual-config
RUN native-image \
    -H:Log=registerResource \
    -H:ReflectionConfigurationFiles=${BASH_SOURCE%/*}/manual-config/reflect-config.json,${BASH_SOURCE%/*}/generated-config/reflect-config.json \
    -H:ResourceConfigurationFiles=${BASH_SOURCE%/*}/manual-config/resource-config.json \
    --verbose \
    --static \
    --report-unsupported-elements-at-runtime \
    --initialize-at-build-time \
    --no-fallback \
    --no-server \
    -jar application.jar

FROM scratch
EXPOSE 8080
COPY --from=build example ${BASH_SOURCE%/*}/
CMD ["/application"]