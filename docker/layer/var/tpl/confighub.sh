# Memory assigned to the ConfigHub service.  It is recommended to assign 4g or more.
export ALLOCATED_MEMORY=${ALLOCATED_MEMORY}

# HTTP and HTTPs ports
export HTTP_PORT=${HTTP_PORT}
export HTTPS_PORT=${HTTPS_PORT}

# Path to the location where all ConfigHub service logs are stored.
export LOG_PATH=${LOG_PATH}

# Specify an override to the default self-signed certificate/keystore.
export KEYSTORE_FILE=${KEYSTORE_FILE}
export KEYSTORE_ALIAS=${KEYSTORE_ALIAS}
export KEYSTORE_PASSWORD=${KEYSTORE_PASSWORD}
