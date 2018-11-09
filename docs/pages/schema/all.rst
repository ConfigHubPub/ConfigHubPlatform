****************
All Repositories
****************

Use this API to get full details of settings for all defined repositories.

- API URL:  ``https://confighub-api/rest/info/all``

.. note:: - All data returned is in JSON format.
   - All dates are expected and returned in ``ISO 8601`` format (UTC): ``YYYY-MM-DDTHH:MM:SSZ``.
   - All parameters are passed through HTTP header fields.
   - Method: GET

Usage
-----

.. code-block:: bash

   curl -i https://api.confighub.com/rest/info/all  \
        -H "Client-Version: v1.5"                   \
        -H "Pretty: true/false"

.. code-block:: json

    HTTP/1.1 200 OK
    Date: Fri, 25 Nov 2016 19:47:39 GMT
    Content-Type: application/json
    Content-Length: 2776
    Server: TomEE
    [
      {
        "account": "ConfigHub",
        "name": "Demo",
        "isPrivate": false,
        "isPersonal": false,
        "description": "This is a demo repository. Saving changes is disabled for all options, however all options are available for you to explore.",
        "created": "2016-05-05T16:26Z",
        "accessControlsEnabled": false,
        "vdtEnabled": true,
        "securityEnabled": true,
        "contextGroupsEnabled": true,
        "keyCount": 27,
        "valueCount": 42,
        "userCount": 1,
        "context": [
          "Environment",
          "Application"
        ]
      },
      {
        "account": "ConfigHub",
        "name": "HowItWorks",
        "isPrivate": false,
        "isPersonal": false,
        "created": "2016-10-04T21:19Z",
        "accessControlsEnabled": false,
        "vdtEnabled": true,
        "securityEnabled": true,
        "contextGroupsEnabled": false,
        "keyCount": 13,
        "valueCount": 23,
        "userCount": 0,
        "context": [
          "Environment",
          "Application"
        ]
      }
    ]


Request Headers
---------------

*Client-Version*

   Version of the client API. If not specified, ConfigHub assumes the latest version. Even through this is
   not a required parameter, you are encouraged to specify a version.

*Pretty*

   If value is ``true``, returned JSON is 'pretty' - formatted.
