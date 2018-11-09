.. _primer:

ConfigHub overview
^^^^^^^^^^^^^^^^^^

All configuration can be boiled down to key-value pairs (properties).  Ignoring the format
that surrounds various configuration components, configuration differences are always reduced to properties.

For example, in **Development** environment, a config file may contain a line like::

    <Connector port="8080" redirectPort="8443"/>

While in **Production** the same line looks like this::

    <Connector port="80" redirectPort="443"/>

Therefore, the merged configuration could be written as::

    <Connector port="${ http.port }" redirectPort="${ http.redirect }"/>

And we have our two properties:  ``http.port`` and ``http.redirect``.


Context properties
~~~~~~~~~~~~~~~~~~

In order to eliminate a mesh of configuration file and property duplication, ConfigHub changes the definition
of a property.  By assigning a context to a property value, a single property key can have multiple values,
each with a unique context signature.

.. note::

   Context Property
      property = key: [ context_1: value, context_2: value, ... ]

When an application/client requests configuration, they only need to specify their context.  Using a request
context, the exact key-value pairing occurs, and the result is returned to the client.

Using example above, a request context ``Production`` would return::

   http.port: 80
   http.redirect: 443

While a request with context ``Development`` would return::

   http.port: 8080
   http.redirect: 8443

In this example, context is very simple - its composed with a single context hierarchy, Environment.  However,
context can be as complex as your environment demands - up to 10 context element hierarchy.


Context resolution
~~~~~~~~~~~~~~~~~~

Context resolution is a process during which value-context of each key is compared to the request context in order
to determine which properties should be returned.

Matching value to request context occurs in two steps:

1. Semantic Filter
------------------

   For each context hierarchy, corresponding context-values from request and property are compared.
   For a match, corresponding context values have to satisfy following rules:

   * If both are specified, they have to be the same;
   * Either or both are a wildcard.

   .. role:: nb
   .. role:: sr
   .. role:: gt


   **Example**: Context-Request resolution

   Assume a context property is defined with for a key ``logger.level`` with 4 values.

   +---------------------+------------------+---------------+---------------+-----------------+
   |                     | Environment      | Application   | Instance      |                 |
   +=====================+==================+===============+===============+=================+
   | Request-Context     | Production       | WebServer     | Webserver-Jim |                 |
   +---------------------+------------------+---------------+---------------+-----------------+

   +---------------------+------------------+---------------+---------------+-----------------+
   | Value-Context       | :nb:`\*`         | :nb:`\*`      | Webserver-Jim | :sr:`Match`     |
   +---------------------+------------------+---------------+---------------+-----------------+
   | Value-Context       | Production       | WebServer     | :nb:`\*`      | :sr:`Match`     |
   +---------------------+------------------+---------------+---------------+-----------------+
   | Value-Context       | Production       | :nb:`\*`      | :nb:`\*`      | :sr:`Match`     |
   +---------------------+------------------+---------------+---------------+-----------------+
   | Value-Context       | :gt:`Development`| :nb:`\*`      | :nb:`\*`      | :gt:`No Match`  |
   +---------------------+------------------+---------------+---------------+-----------------+

   The semantic filter has matched 3 values, and ignored a single value because **Environment**
   context hierarchy from Request-Context "Production" did not match "Development".


2. Weight Filter
----------------

   Weighted filter is only applied if Context-Request is fully-qualified (each context hierarchy is specified).

   As repository's context scope can vary in size (see Choosing Repository Context Scope), each of the context
   blocks is assigned specific weight. The widest scope specifications (left) carry less weight, while most
   specific parts (right) carry most weight.

   For example, in a repository with 3 context hierarchies, weight is assigned as follows::

      Environment [40] | Application [80] | Instance [160]


   This repository might have a property defined with multiple values. Each value-context also has weight.

   **Example**: Fully-Specified Request-Context resolution

   +---------------------+------------------+---------------+---------------+-----------------+-----------------+
   |                     | Environment      | Application   | Instance      | Weight          |                 |
   +=====================+==================+===============+===============+=================+=================+
   | Value-Context       | :nb:`\*`         | :nb:`\*`      | Webserver-Jim | 160             | :sr:`Match`     |
   +---------------------+------------------+---------------+---------------+-----------------+-----------------+
   | Value-Context       | Production       | WebServer     | :nb:`\*`      | 40 + 80 = 120   |                 |
   +---------------------+------------------+---------------+---------------+-----------------+-----------------+
   | Value-Context       | Production       | :nb:`\*`      | :nb:`\*`      | 40              |                 |
   +---------------------+------------------+---------------+---------------+-----------------+-----------------+

   The value with the highest `weight` is matched, as it is the most relevant value for the given context request.

   Here's the ConfigHub property editor view of the same property - with the values expanded.

   .. image:: /images/semanticFilter.png


