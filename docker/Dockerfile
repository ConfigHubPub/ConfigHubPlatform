FROM        openjdk:8-alpine

ARG         VERSION
            # Can set VERSION argument with --build-arg when calling 'docker build'

RUN         apk update && apk add --no-cache \
                bash \
                curl \
                gettext \
                py-pip \
                python \
                wget \
            && pip install supervisor \
            && mkdir /var/log/supervisord \
            # Fetch latest tagged version if not specified
            && if [ -z $VERSION ]; then VERSION=$(curl -s https://api.github.com/repos/ConfigHubPub/ConfigHubPlatform/releases/latest | grep tag_name | grep -oe 'v[\.0-9]*'); fi \
            && wget -q https://github.com/ConfigHubPub/ConfigHubPlatform/releases/download/${VERSION}/confighub-${VERSION}.tar.gz \
            && tar -xzvf confighub-${VERSION}.tar.gz \
            && rm confighub-${VERSION}.tar.gz \
            && mv confighub-${VERSION} confighub \
            && rm /bin/sh && ln -s /bin/bash /bin/sh
            # Fixes a bug where /bin/sh on alpine can't do <<<.

COPY        ./layer /

EXPOSE      80 443
ENTRYPOINT ["/init.sh"]
