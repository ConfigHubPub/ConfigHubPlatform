#!/usr/bin/env bash

if [ "${VERBOSE_STARTUP}" == "yes" ] ; then
  set -x
fi

export ALLOCATED_MEMORY=${ALLOCATED_MEMORY:-4g}
export HTTP_PORT=${HTTP_PORT:-80}
export HTTPS_PORT=${HTTPS_PORT:-443}
export LOG_PATH=${LOG_PATH:-/var/log/confighub}
export KEYSTORE_FILE=${KEYSTORE_FILE:-cert/confighub_default.jks}
export KEYSTORE_ALIAS=${KEYSTORE_ALIAS:-confighub}
export KEYSTORE_PASSWORD=${KEYSTORE_PASSWORD:-confighub}

echo "Waiting for mariadb to start..."
while ! mysqladmin status -h"${DB_HOST}" -u"${DB_SUPERUSER}" -p"${DB_SUPERPASS}"  2>/dev/null ; do
  sleep 1
done

# Ensure confighub can access the database.  Sometimes this can be disrupted when restoring the database server
# from a backup file when running tests.
envsubst < /grant.sql.tpl > /tmp/grant.sql
mysql -h"${DB_HOST}" -u"${DB_SUPERUSER}" -p"${DB_SUPERPASS}" < /tmp/grant.sql || exit $?

echo "Initializing database..."
java -jar /ConfigHubDBManager-${DB_VERSION}.jar -t mysql -r "jdbc:mysql://${DB_HOST}:3306/${DB_NAME}" -u"${DB_USER}" -p"${DB_PASS}" || exit $?

sed -i "s/127.0.0.1/${DB_HOST}/g" /confighub/server/conf/tomee.xml
sed -i "s/<Database_Name>/${DB_NAME}/g" /confighub/server/conf/tomee.xml
sed -i "s/<username>/${DB_USER}/g" /confighub/server/conf/tomee.xml
sed -i "s/<password>/${DB_PASS}/g" /confighub/server/conf/tomee.xml

# Always do this step so the instance always comes up with the latest development package.
WEBAPPS_ROOT=/confighub/server/webapps/ROOT
rm -rf ${WEBAPPS_ROOT} || exit $?
cp /ROOT.war ${WEBAPPS_ROOT}.war || exit $?
echo "Unzipping ROOT.war..."
mkdir ${WEBAPPS_ROOT}
unzip -o ${WEBAPPS_ROOT}.war -d ${WEBAPPS_ROOT} >/dev/null || exit $?

echo "Writing ${WEBAPPS_ROOT}/WEB-INF/resources.xml"
envsubst < /dev-resources.xml.tpl > ${WEBAPPS_ROOT}/WEB-INF/resources.xml || exit $?

supervisord --nodaemon --configuration /etc/supervisord.conf
