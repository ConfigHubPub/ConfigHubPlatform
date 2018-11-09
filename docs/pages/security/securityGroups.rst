.. _security:

Security groups
^^^^^^^^^^^^^^^

Security Group is an abstract wrapper to which properties and/or files may be assigned.  Security Group is
password protected, so any modification of the data contained in them requires authentication before the change is
allowed.

In addition to a password challenge, security group may also be defined with **Encryption** enabled using one of
several provided cyphers.  If enabled, content assigned to a security group will, in addition to being password
protected, be encrypted, using the selected cypher and the security group's password.

.. image:: /images/securityGroup.png


Securing files and properties
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Once the security group is created, you can add any number of properties and files to it.
To assign an existing property to a security group, you need to add a security group to the key.


1. Assignment to a property

.. image:: /images/keySecurityGroup.png



2. Assignment to a file

.. image:: /images/fileSecurityGroup.png

