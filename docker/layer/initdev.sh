#!/usr/bin/env bash

export ALLOCATED_MEMORY=${ALLOCATED_MEMORY:-4g}
export HTTP_PORT=${HTTP_PORT:-80}
export HTTPS_PORT=${HTTPS_PORT:-443}
export LOG_PATH=${LOG_PATH:-/var/log/confighub}
export KEYSTORE_FILE=${KEYSTORE_FILE:-cert/confighub_default.jks}
export KEYSTORE_ALIAS=${KEYSTORE_ALIAS:-confighub}
export KEYSTORE_PASSWORD=${KEYSTORE_PASSWORD:-confighub}

while ! mysqladmin ping -hdatabase -uconfighub -pconfighub ; do
  echo "Waiting for mariadb to start..."
  sleep 1
done
# Initialize the database if it hasn't been already done.
if [[ ! -f /db.init ]] ; then
  java -jar /ConfigHubDBManager.jar -t mysql -r jdbc:mysql://database:3306/ConfigHub -uconfighub -pconfighub || exit $?
  touch /db.init
fi

# Always do this step so the instance always comes up with the latest development package.
rm -rf /usr/local/tomee/webapps/ROOT || exit $?
unzip -o /usr/local/tomee/webapps/ROOT.war -d /usr/local/tomee/webapps/ROOT || exit $?
cp -av /ROOT-js /usr/local/tomee/webapps/ROOT/js || exit $?
cp -v /dev-resources.xml /usr/local/tomee/webapps/ROOT/WEB-INF/resources.xml || exit $?

catalina.sh run
