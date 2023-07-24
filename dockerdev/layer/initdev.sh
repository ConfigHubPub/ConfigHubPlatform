#!/usr/bin/env bash

# Always do this step so the instance always comes up with the latest development package.
WEBAPPS_ROOT=/confighub/server/webapps
rm -rf ${WEBAPPS_ROOT}/*
cp /buildtarget/ROOT.war ${WEBAPPS_ROOT}


# Different DB types need special configuration; do that here
if [ "$DB_TYPE" == "mysql" ]; then

    export DB_DRIVER="com.mysql.jdbc.Driver"
    export DB_URL="jdbc:mysql://${DB_HOST}:3306/${DB_NAME}"

    echo "Waiting for mysql db to start..."
    while ! mysqladmin status -h"${DB_HOST}" -u"${DB_USERNAME}" -p"${DB_PASSWORD}" 2>/dev/null ; do
        sleep 1
    done

elif [ "$DB_TYPE" == "postgresql" ]; then

    export DB_DRIVER="org.postgresql.Driver"
    export DB_URL="jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}"

    echo "Waiting for postgresql db to start..."
    while ! pg_isready -q --host="${DB_HOST}" --username="${DB_USERNAME}"; do
        sleep 1
    done

elif [ "$DB_TYPE" == "oracle" ]; then

    export DB_DRIVER="oracle.jdbc.OracleDriver"
    export DB_URL="jdbc:oracle:thin:@${DB_HOST}:1521:${DB_NAME}"

else
    echo "ERROR: Environment variable DB_TYPE must be set to a valid type"
    exit 1
fi


echo "Initializing database..."
java -jar /ConfigHubDBManager.jar -t "${DB_TYPE}" -r "${DB_URL}" -u"${DB_USERNAME}" -p"${DB_PASSWORD}" || exit $?


echo "Starting confighub service..."
/bin/bash /init.sh
