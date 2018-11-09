***********
Config Pull
***********

With fully specified context, pull configuration from ConfigHub service.
The JSON response may contain key-value pairs, as well as resolved files (as per request).

- API URL (with token):  ``https://confighub-api/rest/pull``
- API URL (no token):  ``https://confighub-api/rest/pull/<account>/<repository>``


.. note:: - All data returned is in JSON format.
   - All dates are expected and returned in ``ISO 8601`` format (UTC): ``YYYY-MM-DDTHH:MM:SSZ``.
   - All parameters are passed through HTTP header fields.
   - Returned data will contain resolved properties and files, unless limited by the ``No-Properties`` or ``No-Files`` flags.
   - Method: GET


Usage
-----

.. code-block:: bash

   curl -i https://api.confighub.com/rest/pull     \
        -H "Client-Token: <token>"                 \
        -H "Context: <context>"                    \
        -H "Application-Name: myApp"               \
        -H "Client-Version: <optional version>"    \
        -H "Tag: <optional tag label>"             \
        -H "Repository-Date: <optional repo date>"

.. code-block:: json

    HTTP/1.1 200 OK
    Date: Fri, 10 Jun 2016 22:38:13 GMT
    Content-Type: application/json
    Content-Length: 2167
    Server: TomEE
    {
      "generatedOn": "06/10/2016 22:38:13",
      "account": "ConfigHub",
      "repo": "Demo",
      "context": "Production;TimeKepper",
      "files": {
        "demo.props": {
          "content": " ... ",
          "content-type": "text/plain"
        },
        "server.xml": {
          "content": " ... ",
          "content-type": "application/xml"
        }
      },
      "properties": {
        "db.host": {
          "val": "prod.mydomain.com"
        },
        "server.http.port": {
          "val": "80"
        },
        "db.name": {
          "val": "ProdDatabase"
        },
        ...
        "db.user": {
          "val": "admin"
        }
      }
    }



Request Headers
---------------

*Client-Token*

   Client token identifies a specific repository. This field is not required if the account and repository
   are specified as part of the URL.


*Context*

   Context for the pull request has to be a fully-qualified-context (each context rank has to be specified -
   no wildcards). Context items are semi-colon delimited, and are ordered in order of have to be in context
   rank order. For example, a repository with context size of 3 levels ``Environment > Application > Instance``
   could be defined as::

   -H "Context:  Production;MyApp;MyAppInstance "


*Repository-Date*

   ISO 8601 date format (UTC) ``YYYY-MM-DDTHH:MM:SSZ`` lets you specify a point in time for which to pull
   configuration. If not specified, latest configuration is returned.

*Tag*

   Name of the defined tag. Returned configuration is for a point in time as specified by the tag. If both
   Tag and *Repository-Date* headers are specified, Repository-Date is only used if the tag is no longer
   available.

*Security-Profile-Auth*

   If a repository is enabled for and uses Security-Profiles (SP) with encryption, choose any of several
   ways to decrypt resolved property values.

   #. Server-Side decryption by providing SP name(s) and password(s):
      - Token is created that specifies SP name/password pairs;
      - SP name/password pairs are specified using this request parameter.

   #. Client-Side decryption is also available by:
      - Use of ConfigHub API in a selected language come functionality for local decryption;
      - A client can implement its own decryption;

   Security-Profile-Auth uses JSON format: ``{'Security-Profile_1':'password', 'Security-Profile_2':'password',...}``

*Client-Version*

   Version of the client API. If not specified, ConfigHub assumes the latest version. Even through this is
   not a required parameter, you are encouraged to specify a version.


*Application-Name*

   This field helps you identify application or a client pulling configuration. Visible in Pull Request tab.

*Include-Comments*

   If value is ``true`` response includes comments for property keys.

*Include-Value-Context*

   If value is ``true`` response includes context of resolved property values.

*Pretty*

   If value is ``true``, returned JSON is 'pretty' - formatted.

*No-Properties*

  If value is ``true`` key-value pairs are not returned. This is useful if you are only interested in
  pulling files, and want to make transaction more efficient.

*No-Files*

  If value is ``true`` resolved files are not returned. This is useful if you are only interested in
  pulling properties, and want to make transaction more efficient.

