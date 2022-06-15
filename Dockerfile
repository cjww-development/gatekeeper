FROM adoptopenjdk/openjdk14

ARG GK_VERSION
ENV VERSION $GK_VERSION

RUN mkdir -p /opt/docker
WORKDIR /opt/docker

RUN echo $VERSION

COPY ./target/universal/gatekeeper-${VERSION}.tgz /opt/docker/
RUN tar zxvf gatekeeper-${VERSION}.tgz -C /opt/docker/
RUN rm /opt/docker/gatekeeper-${VERSION}.tgz

ENTRYPOINT /opt/docker/gatekeeper-${VERSION}/bin/gatekeeper -Dhttp.port=80 -Dversion=${VERSION} -Dlogger.resource=logback-prod.xml -Dlogging.coloured=false