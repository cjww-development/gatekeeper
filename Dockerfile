FROM adoptopenjdk/openjdk14

ARG VERSION

RUN mkdir -p /opt/docker
WORKDIR /opt/docker

RUN echo $VERSION

COPY ./target/universal/gatekeeper-${VERSION}.tgz /opt/docker/
RUN tar zxvf gatekeeper-${VERSION}.tgz -C /opt/docker/

ENTRYPOINT /opt/docker/gatekeeper-${VERSION}/bin/gatekeeper -Dhttp.port=80 -Dversion=${VERSION} -Dlogger.resource=logback-prod.xml -Dlogging.coloured=false