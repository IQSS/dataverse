#!/bin/bash -x
FILES=`find src/main/java/edu/harvard/iq/dataverse/authorization src/main/java/edu/harvard/iq/dataverse/authorization/Permission.java src/main/java/edu/harvard/iq/dataverse/engine/command/Command.java src/main/java/edu/harvard/iq/dataverse/DataverseUser.java src/main/java/edu/harvard/iq/dataverse/DvObject.java src/main/java/edu/harvard/iq/dataverse/engine/command/AbstractCommand.java src/main/java/edu/harvard/iq/dataverse/engine/command/impl/AssignRoleCommand.java src/main/java/edu/harvard/iq/dataverse/engine/command/impl/CreateRoleCommand.java src/main/java/edu/harvard/iq/dataverse/RoleAssignment.java | grep \.java$`;
UML='doc/Architecture/auth-classes.uml'
javadoc -private -quiet -J-DdestinationFile=$UML -J-DcreatePackages=false -J-DshowPublicMethods=true -J-DshowPublicConstructors=false -J-DshowPublicFields=true -doclet de.mallox.doclet.PlantUMLDoclet -docletpath ~/bin/plantUmlDoclet.jar -sourcepath . $FILES
java -jar ~/bin/plantuml.jar -graphvizdot ~/.homebrew/bin/dot -tpng $UML
java -jar ~/bin/plantuml.jar -graphvizdot ~/.homebrew/bin/dot -tsvg $UML
