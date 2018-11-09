.. _files:

*****
Files
*****

Much like context properties, files are structured similarly.  A file path + name act the same as a
property key.  The file schema has context attributed, just as property value.

One distinction a file has over property value is that its content can contain specially annotated
property keys.  When a file is returned as a result of application request, those property keys are
substituted for their resolved values.

So, a file with schema:::

    <attrib name="foo" value="${ foo.bar }">

Will be resolved to ``foo.bar``'s value, whatever it may be.  The file returned to the application will be:::

    <attrib name="foo" value="some value">


.. toctree::
    :titlesonly:

    properties/createProperty
    properties/editDeleteProperty
    properties/manageContextElement
