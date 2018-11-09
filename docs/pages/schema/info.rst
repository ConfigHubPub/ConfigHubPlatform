***************
Repository Info
***************

This API provides information about a specific repository.  It allows for glob syntax search of
configuration files, and context definition.


- API URL (with token):  ``https://confighub-api/rest/info``
- API URL (no token):  ``https://confighub-api/rest/info/<account>/<repository>``


.. note:: - All data returned is in JSON format.
   - All dates are expected and returned in ``ISO 8601`` format (UTC): ``YYYY-MM-DDTHH:MM:SSZ``.
   - All parameters are passed through HTTP header fields.
   - Method: GET

Usage
-----

.. code-block:: bash

   curl -i https://api.confighub.com/rest/info      \
        -H "Client-Token: <token>"                  \
        -H "Repository-Date: <ISO 8601 date (UTC)>" \
        -H "Tag: <repo tag>"                        \
        -H "Client-Version: v1.5"                   \
        -H "Files: true/false"                      \
        -H "Files-Glob: <glob expression>"          \
        -H "Context-Elements: true/false"           \
        -H "Context-Labels: <comma delimited list>" \
        -H "Pretty: true/false"

.. code-block:: json

    HTTP/1.1 200 OK
    Date: Wed, 16 Nov 2016 18:12:33 GMT
    Content-Type: application/json
    Content-Length: 483
    Server: TomEE
    {
       "account": "ConfigHub",
       "repository": "HowItWorks",
       "generatedOn": "11/16/2016 18:12:33",
       "context": [ "Environment", "Application" ],
       "contextElements":
       {
          "Environment": [ "Production", "Development" ],
          "Application": [ "Analytics", "Collector", "WebDashboard" ]
       },
       "files": [
          {
             "name": "nginx2.conf",
             "path": "nginx/nginx2.conf",
             "lastModified": 1479260284272
          }
       ]
    }


Request Headers
---------------

*Client-Token*

   Client token identifies a specific repository. This field is not required if the account and repository
   are specified as part of the URL.

*Repository-Date*

   ISO 8601 date format (UTC) ``YYYY-MM-DDTHH:MM:SSZ`` lets you specify a point in time for which to pull
   repository information. If not specified, latest information is returned.

*Tag*

   Name of the defined tag. Returned information is for a point in time as specified by the tag. If both
   Tag and *Repository-Date* headers are specified, Repository-Date is only used if the tag is no longer available.


*Client-Version*

   Version of the client API. If not specified, ConfigHub assumes the latest version. Even through this is
   not a required parameter, you are encouraged to specify a version.

*Files*

   Boolean flag to indicate if all files should be returned. If *Files-Glob* header is specified, this
   flag is ignored and treated true by default.

*Files-Glob*

   Enables glob expressions while searching for files over their path and name.

*Context-Elements*

   Boolean flag to indicate if all context elements should be returned. If *Context-Labels* header is
   specified, this flag is ignored and treated true by default.

*Context-Labels*

   Limit context elements returned by the list of context labels. Comma delimited list of context labels.

*Pretty*

   If value is ``true``, returned JSON is 'pretty' - formatted.