.. _java_client:

Java
^^^^

Java API provides full interface for configuration pull. In addition it adds several convenience utilities.

`Download Java Client API <https://www.confighub.com/download>`_, read the
`V1.0 JavaDoc <https://www.confighub.com/api/docs/Java/v1/index.html?com/confighub/client/ConfigHub.html>`_.


Usage
-----

.. code-block:: java

    public static void main(String... args)
    {
        String token = "..."
        String context = "Production;WebServer;WebServer-Jim";
        ConfigHub configHub = new ConfigHub(token, context)
                .applicationName("HelloConfigApp");

        CHProperties properties = configHub.getProperties();

        // Backup properties to a local file
        properties.toFile("properties.json");

        int dbPort = properties.getInteger("db.port");
        String dbHost = properties.get("db.host");
        String dbUser = properties.get("db.user");
        String dbPassword = properties.get("db.password");

        // Get a few files
        configHub.requestFile("demo.props");
        configHub.requestFile("foo");

        CHFiles files = configHub.getFiles();
        files.writeFile("demo.props", "demo.props");
        files.writeFile("foo", "foo");
    }


Decrypting values
-----------------

Values can be decrypted by either:
#. Adding a security group to a token;
#. Specifying security group name and password in the API call.

Alternatively, you may also decrypting manually, as the value returned will be encrypted.

This section explains the second option - when you want your application to specify the passwords to
specific security groups.

.. code-block:: java

    ConfigHub configHub = new ConfigHub(token, context)
            .applicationName("HelloConfigApp")
            .decryptSecurityGroup("dbPasswords", "db-pass-123")
            .decryptSecurityGroup("keystore", "key-secret-0");
    CHProperties properties = configHub.getProperties();


With the call from the example above, all values of keys assigned to security groups "dbPasswords" and
"keystore" will be returned decrypted.


Type defined properties
-----------------------

Property keys that have specified type are ready to use as per type definition. For example, a key with
value type defined as Integer, can be processed either as an Integer or a String.

.. code-block:: java

    int dbPort = props.getInteger("db.port"); // as Integer
    Long dbPort = props.getLong("db.port");   // or as Long
    String dbPort = props.get("db.port");     // or as String

Assigning to an incorrect type, for example a Boolean, will throw a ``ClassCastException``.


Default property value
----------------------

Each value ``get(key, default)`` method call can optionally specify a default value.


Saving properties to file
-------------------------

You may choose to locally save your pulled configuration to a file. Configuration is in JSON format.

.. code-block:: java

    CHProperties properties = new ConfigHub(token, context)
            .applicationName("MyAppName")
            .getProperties();
    properties.toFile("/path/to/backup/config.json");


**The resulting JSON configuration file:**

.. code-block:: json

    {
      "context": "Production;TimeKeeper",
      "account": "ConfigHub",
      "repo": "Demo",
      "config": {
        "db.name": {
          "val": "ProdDatabase"
        },
        "db.user": {
          "val": "admin"
        },
        ...
        "db.password": {
          "val": "prod-password"
        }
      }
    }


Reading properties from file
----------------------------

API provides an option to read configuration from a stored JSON file.

.. code-block:: java

    ConfigHub configHub = new ConfigHub(token, context);
    // Load properties from file
    CHProperties properties = configHub.getPropertiesFromFile("/path/to/config.json");

The config file has to have a context key defined in the JSON object root that matches the requested context.
If contexts are not the same, API throws ``ConfigHubException``.


Pulling resolved files from repository
--------------------------------------

You may also pull files with ConfigHub variables substituted for resolved property values through the API.

.. code-block:: java

    ConfigHub configHub = new ConfigHub(token, context);
    configHub.requestFile("conf.properties");
    configHub.requestFile("server.xml");
    configHub.requestFile("log4j2.xml");

    // Pull files from ConfigHub
    CHFiles files = configHub.getFiles();

    // Get file content as a String
    String confProps = files.get("conf.properties");

    // Or save them to a local file
    files.writeFile("log4j2.xml", "/path/to/log4j2.xml");
