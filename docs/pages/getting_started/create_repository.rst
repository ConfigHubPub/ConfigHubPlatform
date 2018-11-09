.. _create_repository:

***************************
Create ConfigHub repository
***************************

Location where all configuration is kept. Much like a repository for code, ConfigHub
repository is a storage for configuration.

A repository can be owned by either a personal account, or an organization.

To create a new repository, once logged in:

#. Click on the **New** link (top-right) and choose *Repository*
#. Choose the owner account and enter the repository name.  *Description is optional*
#. Choose and label your context, and save.


.. _choosing_context:

Choosing repository context
---------------------------

When deciding on a repository context, you are essentially modeling your environments.

There are several factors you will need to consider when choosing the context scope for your
repository. Are you running multiple independent projects, do you have different
environments, multiple applications and do they run several instances?

Even though context scope can always be changed, getting it right the first time will save
you some time in the long run.

To help you determine the right scope, answering these questions might help:

* Do you have different environments (i.e. Development, Production, Test, etc.)?
* Do you have multiple applications?
* Are you running multiple instances (with different configuration) of your applications?

Each time you answered yes, your context scope grew by factor of one. And if you did answer
yes to all of them, your labeled context should look like this:
``Environment | Application | Instance``.



Context hierarchy order
-----------------------

Context is defined in the order of precedence. From the widest scope (left), to the
narrowest scope (right) - similar to the way you would narrow in on a general specification.
For example, to explain a person Jim, you could say:  ``Human > Male > Jim``,
and therefore your repository context would be set up as:  ``Species | Sex | Name``.