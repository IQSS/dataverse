#!/bin/bash -x
java -jar ~/bin/plantuml.jar -graphvizdot ~/.homebrew/bin/dot -tpng components.uml
java -jar ~/bin/plantuml.jar -graphvizdot ~/.homebrew/bin/dot -tsvg components.uml
