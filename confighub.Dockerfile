FROM        java:8-alpine

ENV         VERSION="v1.9.12"
ENV         DB_VERSION="1.9.1"

RUN         apk update && apk add --no-cache \
                wget \
                bash \
                python \
                py-pip \
                gettext \
                less \
                gettext \
                vim \
                tcpdump \
                net-tools \
                mariadb-client \
            && pip install supervisor \
            && mkdir /var/log/supervisord \
            && wget -q https://github.com/ConfigHubPub/ConfigHubPlatform/releases/download/${VERSION}/confighub-${VERSION}.tar.gz \
            && tar -xzvf confighub-${VERSION}.tar.gz \
            && rm confighub-${VERSION}.tar.gz \
            && mv confighub-${VERSION} confighub \
            && rm /bin/sh && ln -s /bin/bash /bin/sh
            # Fixes a bug where /bin/sh on alpine can't do <<<.

COPY        docker/layer /
RUN         wget https://github.com/ConfigHubPub/Database-Manager/releases/download/v${DB_VERSION}/ConfigHubDBManager-${DB_VERSION}.jar
COPY        rest/target/ROOT.war /

EXPOSE      80 443
ENTRYPOINT  ["/confighubdev.sh"]
