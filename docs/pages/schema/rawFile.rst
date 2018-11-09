****************
Single File Pull
****************

With fully specified context, pull a specific file from ConfigHub service.
The response contains raw, resolved configuration file

- API URL (with token):  ``https://confighub-api/rest/rawFile``
- API URL (no token):  ``https://confighub-api/rest/rawFile/<account>/<repository>``


.. note:: - All data returned is in JSON format.
- All dates are expected and returned in ``ISO 8601`` format (UTC): ``YYYY-MM-DDTHH:MM:SSZ``.
   - All parameters are passed through HTTP header fields.
   - Returned data is the content of the resolved file.
   - Method: GET


Usage
-----

.. code-block:: bash

   curl -i https://api.confighub.com/rest/rawFile  \
        -H "Client-Token: <token>"                 \
        -H "Context: <context>"                    \
        -H "File: <absolute path>"                 \
        -H "Application-Name: myApp"               \
        -H "Client-Version: <optional version>"    \
        -H "Tag: <optional tag label>"             \
        -H "Repository-Date: <optional repo date>"




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

