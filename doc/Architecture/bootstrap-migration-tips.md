Bootstrap Migration Tips
========================

* `p:panel` -> `ui:fragment`, or just the content, when possible.
* Whenever possible, no xhtml at all
	* `<h:outputText value="abc"/>` -> abc
* conditional rendering based on `ui:fragment`'s `rendered` property.
* Cant use HTML entities. Use the actual unicode char instead.
* HTML5's attributes using passthrough:
	
	h:inputText id="email" value="#{bean.email}">
	<f:passThroughAttribute name="type" value="email"/>
	<f:passThroughAttribute name="placeholder"
		value="Enter email"/>
	</h:inputText>

* More on html5 and JSF 2.2 at http://jsflive.wordpress.com/2013/08/08/jsf22-html5/
* We have a bootstrap component lib, `iqbs`.
* Need to manually convert the font references to JSF compliant:
	* from `url('../fonts/glyphicons-halflings-regular.eot');'
	* to `url("#{resource['bs/fonts/glyphicons-halflings-regular.eot']}");`
	* from `url('../fonts/glyphicons-halflings-regular.eot?SomeThings');' (note the parameter at the end)
	* to `url("#{resource['bs/fonts/glyphicons-halflings-regular.eot']}?someThings");`
	* same for `?` parameters at the end.
	