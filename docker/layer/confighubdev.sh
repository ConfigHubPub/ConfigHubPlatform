#!/usr/bin/env bash

export ALLOCATED_MEMORY=${ALLOCATED_MEMORY:-4g}
export HTTP_PORT=${HTTP_PORT:-80}
export HTTPS_PORT=${HTTPS_PORT:-443}
export LOG_PATH=${LOG_PATH:-/var/log/confighub}
export KEYSTORE_FILE=${KEYSTORE_FILE:-cert/confighub_default.jks}
export KEYSTORE_ALIAS=${KEYSTORE_ALIAS:-confighub}
export KEYSTORE_PASSWORD=${KEYSTORE_PASSWORD:-confighub}

while ! mysqladmin ping -h${DB_HOST} -u${DB_SUPERUSER} -p${DB_SUPERPASS} ; do
  echo "Waiting for mariadb to start..."
  sleep 1
done

# Ensure confighub can access the database.  Sometimes this can be disrupted when restoring the database server
# from a backup file when running tests.
envsubst < /grant.sql.tpl > /tmp/grant.sql
mysql -h${DB_HOST} -u${DB_SUPERUSER} -p${DB_SUPERPASS} < /tmp/grant.sql || exit $?

# Initialize the database if it hasn't already been done.
echo "show tables like 'email';" > /tmp/db.init
mysql -h${DB_HOST} -u${DB_USER} -p${DB_PASS} -D${DB_NAME} < /tmp/db.init | grep email >/dev/null
if [[ $? -ne 0 ]] ; then
  echo "Initializing database..."
  java -jar /ConfigHubDBManager.jar -t mysql -r jdbc:mysql://${DB_HOST}:3306/${DB_NAME} -u${DB_USER} -p${DB_PASS} || exit $?
else
  echo "Database previously initialized."
fi

# Always do this step so the instance always comes up with the latest development package.
WEBAPPS_ROOT=/usr/local/tomee/webapps/ROOT
rm -rf ${WEBAPPS_ROOT} || exit $?
echo "Unzipping ROOT.war..."
unzip -o ${WEBAPPS_ROOT}.war -d ${WEBAPPS_ROOT} >/dev/null || exit $?
cp -av /ROOT-js ${WEBAPPS_ROOT}/js || exit $?

echo "Writing ${WEBAPPS_ROOT}/WEB-INF/resources.xml"
envsubst < /dev-resources.xml.tpl > ${WEBAPPS_ROOT}/WEB-INF/resources.xml || exit $?

catalina.sh run
