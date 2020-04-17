<?xml version="1.0" encoding="UTF-8"?>
<Resources>
    <Resource id="ConfigHubMainDS" type="DataSource">
        JdbcDriver=com.mysql.jdbc.Driver
        JdbcUrl=jdbc:mysql://${DB_HOST}:3306/${DB_NAME}?useSSL=false&amp;autoReconnect=true
        UserName=${DB_USER}
        Password=${DB_PASS}

        JtaManaged = false
        validationQuery = SELECT 1
        maxWaitTime = 2 seconds
        maxActive = 200
    </Resource>
</Resources>
