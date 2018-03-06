# ConfigHub Platform [![Build Status](https://travis-ci.org/ConfigHubPub/ConfigHubPlatform.svg?branch=master)](https://travis-ci.org/ConfigHubPub/ConfigHubPlatform)

Full [Installation instructions](http://docs.confighub.com/en/latest/pages/getting_started.html) are available at [docs.confighub.com](http://docs.confighub.com/en/latest)

## System requirements

The ConfigHub server application has the following prerequisites:

- Runs on Windows/macOS/Linux distribution (Debian Linux, Ubuntu Linux, or CentOS recommended)
- MySQL 5.0 or later, or PostgreSQL 9 or later (latest stable version is recommended)
- Oracle Java SE 8 or later (latest stable update is recommended)

## Upgrading from a previous version?

Please read the release notes - database schema upgrades may be required.


## Docker ConfigHub Platform
A docker image for [ConfigHub](https://www.confighub.com/).

Execute with following:
```
docker run -d \
    -p 8080:80 \
    -p 8443:443 \
    -e DB_URL=jdbc:mysql://<databaseHost>:3306/ConfigHub \
    -e DB_DRIVER=com.mysql.jdbc.Driver \
    -e DB_USERNAME=<username> \
    -e DB_PASSWORD=<password> \
    --name confighub \
    confighub/confighubplatform:latest
```

You may also use PostgreSQL database with
```
    -e DB_DRIVER=org.postgresql.Driver
    -e DB_URL=jdbc:postgresql://<databaseHost>:5432/ConfigHubPSQL
```

And you may specify parameters from the `/confighub-<version>/confighub.sh`.  Shown are default values
which will be applied if these parameters are omited:
```
    -e ALLOCATED_MEMORY=4g
    -e HTTP_PORT=80
    -e HTTPS_PORT=443
    -e LOG_PATH=/var/log/confighub
    -e KEYSTORE_FILE=cert/confighub_default.jks
    -e KEYSTORE_ALIAS=confighub
    -e KEYSTORE_PASSWORD=confighub
```

After the docker container starts, you can access the web interface on `https://localhost:8080`.
