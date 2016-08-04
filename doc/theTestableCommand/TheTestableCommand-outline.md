# The Testable Command

_outline_
* Intro
  *√ Application Complexity
  *√ Definitions of unit tests
    *√ Positive def.
      *√ Quick runs
      *√ validate small portions of limited complexity
      *√ Use the code in another context (aids portability, reuse, and, thus, overall quality)
      *√ To some extent, can be read as a use guide and a specification
    *√ No dependency on other processes
    *√ No dependency on external files
    *√ No dependency on hard-coded data that needs to be manually changed
    * running under JUnit not enough
    * Make sure to test the right thing, and to test the thing right (e.g. no `toString` equality, unless you're testing some logic that generates Strings).
  *√ Why no embedded test servers
    *√ Too many moving parts
    *√ Part code, part spec, part magic (e.g. putting stuff in `private` fields!)
      * Mention the Weld bug

* Commands in Dataverse
  *√ Command pattern
    *√ Refer to the "Lean Beans are Made of This" presentation
  * Making a command testable - what to do in the service bean and what should be done in a command
    * √Command should not deal directly with anything that's not a service bean
      or a domain model object - including the entity manager, API calls to Solr, file system calls,
      HTTPRequest, JSFContext, etc.
      * √ This roughly amounts to - Storage and retrieval "primitives" (of models) go in the bean, actions go on the commands.
      * True, I've added the `em()` method to the `CommandContext` class. That was
        while exploring the idea of removing the beans altogether. It works, but
        its not testable. So it will be deprecated at some point.
    *√ Any context object (JSFContext, HTTPRequest) should not be used by the command. Extract exactly what the command needs, and pass it as a parameter to the command's constructor.
      * x e.g. `DataverseRequest` had a constructor that got a `HTTPRequest` as a parameter. Internally, that constructor extracted the source IP address and stored it in a field. To allow testing, a new constructor, one that gets only the IPAddress, was added.

* Testing the command
  * Setting up the domain context in on which the command acts
    * Dataverses, Datasets....
    * Use `MocksFactory` (lives in the test folder, not in src) to create sensible default objects.
    * Hand-craft the instances needed for the test, to make sure the test case really tests what it needs to test.
  * Create a `TestCommandContext` subclass, and override the methods providing the required service beans. The service beans might need to be subclassed as well, typically replacing database calls with actions on in-memory data structures.
  * Create a `TestDataverseEngine` instance, and pass it an instance of the `TestCommandContext` subclass.
  * Submit the command
  * `Assert` ad nauseum.
    * Command results
    * Calls within the beans (e.g. validating that a method did get called, or not called more than once)
    * Permissions required by the command
