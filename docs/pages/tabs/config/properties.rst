.. _properties:

*****************
Properties editor
*****************

Properties are traditionally defined as:::

    key: value

In ConfigHub, a property is defined with a context attached to the value.  This schema allows
many values to be assigned to a single key, with the requirement that each value's context
is unique among other values assigned to the same key.::

    key: [
        context: value,
        context: value,
        ...
    ]

Following sections show how context properties and context elements are created, edited and deleted.


Editor toolbar
^^^^^^^^^^^^^^

Properties editor is the default view of ConfigHub UI where all/resolved properties are managed.
The properties toolbar allows provides following functionality:

.. image:: /images/propertiesToolbar.png


1. **Context selection**

    These fields allow you to set the working context, and view configuration that is "resolved" by that context.
    Unlike the context requested by the client, editor context can have multiple context elements specified in
    a single context hierarchy.

    For example, if you wanted to see configuration returned for both *Production* and *Development* environments,
    you could select both in the Environment context hierarchy.  The returned configuration would show all
    keys and values that can be returned for the combination of the specified context.

    We call this type of context - where any number of context elements can be selected per context hierarchy, a
    **non-qualified context**;


2. **Search type**

    Searching keys, comments or values, may return results either among the resolved values (as indicated by
    specified context #1), or among all configuration properties in this repository.

    The **All** selection, searches all properties in the repository, where **Resolved** searches only among properties
    resolved by the specified context.


3. **New property**

    This button toggles the new property form, where a new or existing key can be specified as well as a property value.

4. **Comparison View**

    Side-by-side comparison of resolved properties from any combination of contexts, times or tags.

5. **All key toggle**

    Let's you see all property keys.  If some keys did not resolve values as per the specified context #1, these
    keys will not be shown in the properties editor.  Clicking this key, will include them in the display, but
    their values are still left out of the view.

6. **Key sort order**

    Keys are sorted alphabetically.  Sort order toggles direction.

7. **Value context alignment**

    To see all value contexts aligned as a table view, toggle this button.

8. **Pagination navigation**

    Move between pages of results.






Create a new property
^^^^^^^^^^^^^^^^^^^^^

.. image:: /images/newProperty.png

1. **Readme**

    Comment for the property key.  Visible and searchable in property editor.

2. **Key Attributes**

    - **Type:** Refers to the data type of values.  When a non-Text value is selected, this value type is enforced in the UI, and the type information is included in the JSON API response.

    - **Security:** Chose pre created *Security Group*.  All values assigned to the key will adhere to access controls set by the Security Group, and will be encrypted as per Security Group settings.

    - **Push:** If enabled, this key and its values may be modified via PUSH API.

3. **Key**

    Property key is unique per repository.  Specifying a key that is already defined, will just add new value to the
    existing key.  **Deprecated** flag can be enabled for a key.  If enabled, client PULL response JSON will include
    a deprecated flag on the key object.  Use this flag to log all deprecated key usages.

4. **Value**

    Value for this key.  If *Type* (key attribute) flag is specified, value input field is changed in consideration of
    the value type.

5. **Context**

    Each key's value has to have a unique context.  Having a unique context guarantees that a fully-specified-context,
    as requested by the client applications, can only receive back a single value per key.

    Each context's hierarchy may contain a wildcard or a single context element.

6. **Save or Save with change comments**

    Clicking on a talk-bubble button next to the "Save property" will pop-open a "Change comment" text box.  Comments
    entered here will be visible next to the change in the *Revisions* tab.






Add value to existing key
^^^^^^^^^^^^^^^^^^^^^^^^^

Mousing over the existing key, attributes or values, shows additional options for the key.

.. image:: /images/entry.png

Choosing *New value* will open a new value form.

.. image:: /images/newValue.png

The value's form elements are the same as specified in `Create a new property`_ section.
Additional option is the **Active** toggle.  When a value is *Disabled*, it is treated as if it is deleted.
A *Disabled* value is never returned to the client.



Editing keys and values
^^^^^^^^^^^^^^^^^^^^^^^

Double clicking on a key or a value, will open the editing form for either key or a value.
You can also click on *Edit key* from the key's *options* menu.  Clicking the right array next to the value
will also trigger the value form.



