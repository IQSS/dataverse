The following needs to be added to the release notes and/or upgrade instructions: 

If you are using Web Analytics, please review your "analytics-code.html" fragment (described in [Installation Guide > Configuration > Web Analytics Code](http://guides.dataverse.org/en/latest/installation/config.html#web-analytics-code)), and see if any of the script lines contain an *empty* "async" attribute. In the documentation provided by Google, its value is left blank \
(as in ``<script async src="...">``). It must be set to "async" explicitly (for example, ``<script async="async" src="...">``), otherwise it may cause problemw with some pages/browsers.
