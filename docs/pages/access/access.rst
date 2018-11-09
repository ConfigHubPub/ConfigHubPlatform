.. _access:

Team Access Control
^^^^^^^^^^^^^^^^^^^

Team access rules let repository owners/admins set write limits to both configuration properties and files for each team.
The rules defined limit users ability to create/modify/delete properties and files.  The rules apply when manipulating
config via UI as well as API.

For access rules to apply via API, a token needs to apply access rules for a specific team.

To enable access controls for teams, go to::

    Settings > Configuration > Access control management

Once enabled, you can create rules that will govern Read/Write privileges for members of each team.


Create team access rule
-----------------------

To create a new access rule, select a team and click "new" next to "Team rules" section.

.. image:: /images/newRule.png

You can add any number or rules to each team.  Rules will govern write privileges to both properties and files in UI
as well as the API Push when token assigned to a team/team member is used.


Key Rule
========

Key rules are matched against a property key only
1. Select Read/Write or Read-Only option that will be applied if a rule is matched against a key
2. Select "Key"
3. Choose matching type when comparing keys against a match string you will specify
4. Enter a matching string
5. Click "Create Rule"



Context Rule
============

Context rules are matched against property values and files.

1. Select Read/Write or Read-Only option that will be applied if a rule is matched against a property and/or file
2. Select "Context"
3. Choose matching type:
   - "Contains Any" - if any context elements specified in the rule is found in the context definition of a property value's or file's context, rule is matched.
   - "Contains All" - all context elements specified have to be present in property value's or file's context.
   - "Does not contain" - property value's or file's context cannot have any context elements found in this rule to match.
   - "Resolves" - if this rule's context resolves the context of the property value or a file, rule will match.
   - "Does not resolve" - property value's or file's context is not resolved by this context query.
4. Specify context elements
5. Click "Create Rule"
