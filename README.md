# ConfigHub Platform [![Build Status](https://github.com/ConfigHubPub/ConfigHubPlatform/actions/workflows/build.yml/badge.svg)](https://github.com/ConfigHubPub/ConfigHubPlatform/actions/workflows/build.yml) [![Docker Status](https://github.com/ConfigHubPub/ConfigHubPlatform/actions/workflows/docker.yml/badge.svg)](https://github.com/ConfigHubPub/ConfigHubPlatform/actions/workflows/docker.yml)

Full [Installation instructions](http://docs.confighub.com/en/latest/pages/getting_started.html) are available at [docs.confighub.com](http://docs.confighub.com/en/latest)

## System requirements

The ConfigHub server application has the following prerequisites:

- Runs on Windows/MacOS/Linux distribution (Debian Linux, Ubuntu Linux, or CentOS recommended) or as a docker image
- Supported databases: MariaDB/MySQL, PostgreSQL, Oracle 11 or later
- Tested with Oracle Java 8

## Database Schema

Schema is liquibase managed by an application found in [Database-Manager](https://github.com/ConfigHubPub/Database-Manager) 
repo.  A pre-compiled JAR is included with each ConfigHub Platform release. 

## Issue Tracking

Found a bug? Have an idea for an improvement? Feel free to [add an issue](../../issues).


## Contributing

Help us build the future of configuration management and be part of a project that is changing how 
configuration is leveraged across teams every day.

Follow the [contributors guide](https://www.confighub.org/community) and read [the contributing instructions](CONTRIBUTING.md) to get started.



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
