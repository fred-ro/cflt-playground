ARG KSQL_VERSION=0.28.2
FROM confluentinc/ksqldb-server:${KSQL_VERSION}
ARG KSQL_VERSION

USER root
RUN dnf install -y https://dl.fedoraproject.org/pub/epel/epel-release-latest-8.noarch.rpm ; \
    dnf install -y jemalloc ; \
    dnf clean all

USER appuser
RUN cd /usr/share/java/ksqldb-rest-app && \
    echo "Downloading ksqldb-rocksdb-config-setter-${KSQL_VERSION}.jar" && \
    curl -O https://ksqldb-maven.s3-us-west-2.amazonaws.com/maven/io/confluent/ksql/ksqldb-rocksdb-config-setter/${KSQL_VERSION}/ksqldb-rocksdb-config-setter-${KSQL_VERSION}.jar
ENV LD_PRELOAD=/usr/lib64/libjemalloc.so.2