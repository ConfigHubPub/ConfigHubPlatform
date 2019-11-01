FROM       tomee:8-jre-7.0.6-plume

COPY       docker/layer /
COPY       docker/ConfigHubDBManager.jar /

RUN        apt-get update && \
           apt-get -y install \
               vim \
               tcpdump \
               net-tools \
               mariadb-client

EXPOSE     80 443
ENTRYPOINT ["/initdev.sh"]
