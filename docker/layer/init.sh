#!/usr/bin/env bash

if [ -z ${DB_TYPE+x} ]; then echo "DB_TYPE not set!" exit 1; fi
if [ -z ${DB_URL+x} ]; then echo "DB_URL not set!" exit 1; fi
if [ -z ${DB_USERNAME+x} ]; then echo "DB_USERNAME not set!" exit 1; fi
if [ -z ${DB_PASSWORD+x} ]; then echo "DB_PASSWORD not set!" exit 1; fi

case ${DB_TYPE} in
    mysql)      export DB_DRIVER="com.mysql.jdbc.Driver" ;;
    postgresql) export DB_DRIVER="org.postgresql.Driver" ;;
    oracle)     export DB_DRIVER="oracle.jdbc.OracleDriver" ;;
    *)          echo "DB_TYPE not valid!" && exit 1
esac

echo "Writing database credentials to /confighub/server/conf/tomee.xml"
envsubst < /var/tpl/tomee.xml > /confighub/server/conf/tomee.xml

export ALLOCATED_MEMORY=${ALLOCATED_MEMORY:-4g}
export HTTP_PORT=${HTTP_PORT:-80}
export HTTPS_PORT=${HTTPS_PORT:-443}
export LOG_PATH=${LOG_PATH:-/var/log/confighub}
export KEYSTORE_FILE=${KEYSTORE_FILE:-cert/confighub_default.jks}
export KEYSTORE_ALIAS=${KEYSTORE_ALIAS:-confighub}
export KEYSTORE_PASSWORD=${KEYSTORE_PASSWORD:-confighub}

echo "Writing server configuration to confighub/confighub.sh"
envsubst < /var/tpl/confighub.sh > /confighub/confighub.sh

echo "Initializing database..."
java -jar /ConfigHubDBManager.jar -t "${DB_TYPE}" -r "${DB_URL}" -u"${DB_USERNAME}" -p"${DB_PASSWORD}" || exit $?

echo "Starting service..."
supervisord --nodaemon --configuration /etc/supervisord.conf
