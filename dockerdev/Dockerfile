FROM        openjdk:8-alpine

RUN         apk update && apk add --no-cache \
                bash \
                curl \
                gettext \
                less \
                mariadb-client \
                net-tools \
                postgresql-client \
                py-pip \
                python \
                tcpdump \
                vim \
                wget \
            && pip install supervisor \
            && mkdir /var/log/supervisord \
            && rm /bin/sh && ln -s /bin/bash /bin/sh
            # Fixes a bug where /bin/sh on alpine can't do <<<.

RUN         curl -s https://api.github.com/repos/ConfigHubPub/ConfigHubPlatform/releases/latest \
               | grep browser_download_url \
               | grep -o http.*\.tar\.gz \
               | wget -qi - \
            # Gets the latest ConfigHubPlatform release .tar.gz from github
            && tar -xzvf confighub-*.tar.gz \
            && rm confighub-*.tar.gz \
            && mv confighub-* confighub

RUN         curl -s https://api.github.com/repos/ConfigHubPub/Database-Manager/releases/latest \
               | grep browser_download_url \
               | grep -o http.*\.jar \
               | wget -O ConfigHubDBManager.jar -qi -
            # Gets the latest Database-Manager release .jar from github

COPY        ./docker/layer /
COPY        ./dockerdev/layer /

EXPOSE      80 443

ENTRYPOINT  ["/initdev.sh"]
