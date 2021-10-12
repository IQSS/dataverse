====================
UI Pages Development
====================

While most of the information in this guide focuses on the development of service and backing beans in Java, working on JSF/Primefaces xhtml pages presents its own unique challenges. 

.. contents:: |toctitle|
	:local:

Avoiding Inefficiencies in JSF render logic
-------------------------------------------

It's important to keep in mind that the expressions in JSF "rendred=" attributes may be evaluated **multiple** time. So it is crucial not to use any expressions that require database lookups, or otherwise take any appreciable amounts of time and resources. Ideally render attributes should only contain call to methods in backing beans or caching service wrappers that perform any required lookups or evaluation on the first call, then keep returning the cached result on all the consecutive calls. This way it is irrelevant how many times PrimeFaces may need to call the method as any effect on the performance is negligible.

If you are ever in doubt how many times the method in your render logic expression is called, you can simply add a logging statement to the method in question. Or you can simply err on the side of assuming that it's going to be called a lot, and ensure that any repeated calls are expensive to process.

A simplest, trivial example would be a direct call to a method in SystemConfig service bean. For example, 

``<h:outputText rendered="#{systemConfig.advancedModeEnabled}" ...``

If this method (``public boolean isAdvancedModeEnabled()`` in ``SystemConfig.java``) consults a database setting every time it is called, this database query will be repeated every time JSF reevaluates the line above. This is obviously not an expensive lookup, but repeated enough times unnecessary queries do add up, especially on a busy server. So instead of SystemConfig, SettingsWrapper (a ViewScope bean) should be used instead to cache the result on the first call:

``<h:outputText rendered="#{settingsWrapper.advancedModeEnabled}" ...``

with the following code in ``SettingsWrapper.java``:

.. code:: java
	  
	  private Boolean  advancedModeEnabled = null; 
	  
	  public boolean isAdvancedModeEnabled() {
	     if (advancedModeEnabled == null) {
                advancedModeEnabled = systemConfig.isAdvancedModeEnabled();
             }
             return advancedModeEnabled; 
          }

A more serious (real life) example would be direct calls to PermissionServiceBean methods used in render logic expression. A simple permission service lookup (for example, whether a user is authorized to create a dataset in the current dataverse) can easily take 15 database queries. Repeated multiple times, this can quickly become a measurable delay in rendering the page. PermissionsWrapper must be used exclusively for any such lookups from JSF pages.

