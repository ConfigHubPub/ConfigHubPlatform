.. _propertiesToolbar:

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
    Comparison view is a side-by-side view of the properties from either/or different contexts or time/tag.

5. **All key toggle**
    Let's you see all property keys.  If some keys did not resolve values as per the specified context #1, these
    keys will not be shown in the properties editor.  Clicking this key, will include them in the display, but
    their values are still left out of the view.

6. **Key sort order**

7. **Value context alignment**
    To see all value contexts aligned as a table view, toggle this button.

8. **Pagination navigation**
    Move between pages of results.