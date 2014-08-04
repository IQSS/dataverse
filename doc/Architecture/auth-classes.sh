#!/bin/bash -x
FILES=`ls -1 src/main/java/edu/harvard/iq/dataverse/authorization/*.java | grep -v Main.java`;
UML='doc/Architecture/auth-classes.uml'
javadoc -private -quiet -J-DdestinationFile=$UML -J-DcreatePackages=false -J-DshowPublicMethods=true -J-DshowPublicConstructors=false -J-DshowPublicFields=true -doclet de.mallox.doclet.PlantUMLDoclet -docletpath ~/bin/plantUmlDoclet.jar -sourcepath . $FILES
java -jar ~/bin/plantuml.jar -graphvizdot ~/.homebrew/bin/dot -t png $UML
java -jar ~/bin/plantuml.jar -graphvizdot ~/.homebrew/bin/dot -t svg $UML
