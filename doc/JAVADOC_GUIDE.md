# Javadoc Guidelines

Javadoc is the main tool for documenting code written in Java. It is not the only documentation method, though - there are also block comments (`/* ... */`) and line comments (`// ... `). And, of course, there is external documentation - presentations, readme files, etc. Each documentation method has its own target audience. External documentation works well for onboarding developers. Block and line comments work well for addressing maintenance programmers (hopefully, a future you). Javadoc was created mostly for addressing client code programmers - people that write code that uses the documented code.
A Javadoc comment (`/** ... */`) is always attached to an element of the code.

## Style guide
First and foremost, consider your audience. People mostly read the Javadocs when they are using the code as a black box, so describe what the documented artifact does from a client code's point of view. The description should be accurate, but short. The first sentence should be a summary - it's not just a good idea, it's the sentence copied by the Javadoc tool into summary tables at the top of the each Javadoc page. E.g., the table at the project overview page contains the first sentence from the Javadoc comment of each package. Note, however, that the end of the first sentence is detected by looking for the first period followed by a white space. Thus, abbreviations such as "e.g." would be considered as terminating a sentence. The standard workaround is to omit the last period, i.e. "e.g".

Subsequent paragraphs may include references to the implementation, if needed. Describe the current state of the code, not its history. While Javadoc allows embedding links to external resources, do not rely heavily on these, as they can change. When a diagram would do a better job than text, consider using ASCII drawings.

Javadoc supports links and formatting using HTML and specialized Javadoc tags, written `{@tagName tagParameter}`.  The full list of Javadoc tags is maintained at the [Javadoc main page](http://docs.oracle.com/javase/8/docs/technotes/tools/windows/javadoc.html). Important tags include:

* `{@code javaCodeGoesHere}` for adding inline code - very important when referencing code that has generics, since Java's type parameter notation (`List<String>`) will confuse the HTML parser unless escaped.
* `{@see reference}` a link to another class, method, or field in the documentation, appears at the end of the Javadoc comment.
* `{@link className}` same as above, but inline.

As for the HTML tags, try to stick with the textual formatting ones (`<em>`,`<ol>`,`<table>`...), and avoid using structural ones (`<div>`, `<iframe>`). Using the header tags (`<h1>...<h6>`) is discouraged, as they interfere with the semantic structure of the generated document.

Writing good Javadocs is hard, as it requires the writer to phrase exactly what the code does. For this reason, it may be useful to write the Javadoc prior to writing the actual code it documents, as a way of arranging one's thoughts. This is a relaxed version of TDD, where test are written before code.

Lastly, don't overdo it - documentation has to be maintained and updated with the code. Outdated documentation may be even worse than not having one at all.


### Packages
To document a package, create a file called `package-info.java` in the package's directory, and add the comment above the package declaration in that file. This type of documentation is rarely seen in the wild, but is very useful to have when a package usage is not obvious, or when a package has multiple entry points.

    /**
     * This package contains all that's needed to do that complicated
     * thingy. There are a few ways of getting it done, based on your
     * need and starting point. If you're running in a JavaEE container,
     * consider using the {@link ContainerGetThingDoneer} as your starting
     * point. Otherwise, use {@link StandaloneGetThingDonner}.
     */
    package edu.harvard.iq.non.intuitive.stuff;

### Classes

Explain what this class does and how it should be used. Consider using sample code. If the class has non-private static methods, make sure to distinguish them from their instance counterparts. Provide guidance regarding when to use which.

    /**
     * Orchestrates the doing of thing in a container environment
     * (e.g Glassfish 4.1). Client code can obtain a configured instance
     * using the static method {@link #create()}, or use one of the class'
     * constructors and configure the instance manually.
     */
     @AnAnnotation
     public class ContainerGetThingDonner {
     ...

### Methods

Method documentation is structured, supporting special tags for the parameters (`@param <name> <description>`) and the return value (`@return <description>`). Together with the method's name, return type, and names and types of the method parameters, most methods can be sufficiently documented without adding free text to the comment. Note, however, that omitting this text would mean no description at the method's row in the summary table at the top of the class page. While documenting methods, use the `@throws` tag to document which exceptions may be thrown, and under which circumstances. This is especially important for runtime exceptions, which are not stated in the method's signature.

A full method javadoc is great, but it may read a bit verbose:

    /**
     * Creates a description of {@code this}, using at least {@code minimalWordCount} words.
     * @param minimalWordCount Lower bound for the generated description word count. Must be a positive number.
     * @return A description of {@code this} object, at least {@code minimalWordCount} words long.
     * @throws IllegalArgumentException when {@code minimalWordCount} is equal to, or less than, 0.
     * @see an.other.package.Description#createFor( java.lang.Class, int )
     */
    public String createDescription( int minimalWordCount ) {
    ...

Since most developers read the Javadocs through their IDE's documentation pane, and not the generated HTML, this may suffice:

    /**
     * @param minimalWordCount
     * @return A description of {@code this} object, at least {@code minimalWordCount} words long.
     * @throws IllegalArgumentException when {@code minimalWordCount} is equal to, or less than, 0.
     * @see an.other.package.Description#createFor( java.lang.Class, int )
     */
    public String createDescription( int minimalWordCount ) {
    ...

Note that a `@param` tag for `minimalWordCount` exists, but has no description. This is the common way of saying "I did not forget to document this parameter - its name and type sufficiently describe its role".

### Fields

Java allows for multiple fields to be defined using a single statement. This, however, collides with Javadoc's "Javadoc comment precedes the element it documents" principle. When a Javadoc comment appears before a statement defining multiple fields, it is copied to both of them, which it probably not the desired behavior. In the following example of a backing bean for a page that allows moving a dataset between two dataverses, both the source and the target dataverse will have the same Javadoc comment:

    /** Datasets on this page */
    private Dataverse destinationDataverse, sourceDataverse;  // bad idea

Here's a better approach:

    /** The dataverse we move the dataset <em>from</em> */
    private Dataverse sourceDataverse;

    /** The dataverse we move the dataset <em>to</em> */
    private Dataverse destinationDataverse;


## Links

* [Javadoc](http://docs.oracle.com/javase/8/docs/technotes/tools/windows/javadoc.html)
* [PlantUML's ASCII diagrams](http://plantuml.com/ascii_art.html)
* [JavE - Free ASCII art tool](http://www.jave.de/)
