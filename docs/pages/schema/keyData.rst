****************
Single Key Data Definition Pull
****************

With a given key name, pull a specific key's definition.
The response contains a list of key value pairs the given key definition.

- API URL (with token):  ``https://confighub-api/rest/keyData``
- API URL (no token):  ``https://confighub-api/rest/keyData/<account>/<repository>``


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
        -H "KeyName: myKeyName"                    \
        -H "Application-Name: myApp"               \
        -H "Client-Version: <optional version>"    \
        -H "Tag: <optional tag label>"             \
        -H "Repository-Date: <optional repo date>" \
        -H "Pretty: <optional boolean>" \


Request Headers
---------------

*Client-Token*

   Client token identifies a specific repository. This field is not required if the account and repository
   are specified as part of the URL.


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

*Pretty*

   If value is ``true``, returned JSON is 'pretty' - formatted.

