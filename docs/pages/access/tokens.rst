.. _tokens:

Tokens
^^^^^^

Repository tokens are used by the client APIs.  Optionally, a repository could be set up to allow API access without
a token (Repository > Settings > Configuration > API Settings), by default,  token is required for both API push and
pull.

A ConfigHub token is a JSON JWT Web token, following industry standard RFC 7519 method for representing claims securely
between two parties.  In its payload, it contains only a repository id.  Once the token is received by a ConfigHub
instance, its validity, expiration and active flags are checked.  Content is only returned of all checks are validated.

Tokens with a team assigned to "Push Access Rules", will prevent modification to configuration if any rule from the
team prevents write access.


Creating a token
================

.. image:: /images/token.png

1. Token name has to be unique among tokens.
2. Expiration date is optional.  If set, token will no longer be accepted after the expiration is reached.
3. Push Key Override - if enabled will allow property value Push via API, regardless of the property key's Push setting.
4. Push Access Rules - let's you specify a team.  Access rules defined for the team will be applied for all API Push requests issued by the client.
5. Security Groups entered into this field will pre-authorize the token to decrypt values and files assigned to them prior to returning the data via API Pull request.
6. Managed By - let's you specify who has visibility to this token.


Adding security groups to tokens
================================

.. image:: /images/tokens.png

Security Groups can be added to a token at creation time, or any time thereafter.  Furthermore, all repository
members may edit any other token to add/remove security groups from them.  As long as they can authenticate to the
security group, they can adjust any other token.

The intention is that, if need be, a user which knows a password to a security group can extend access to
config that is protected/encrypted by the security group without sharing security group's the password with another
user - token owner.

Deleting a token
================

A personal token, where "Managed By" is "Only You" can be deleted by the token's owner and repository owner/admin.
All other tokens - those assigned to teams, admins, or all, can be delete by the repository owner/admin.

