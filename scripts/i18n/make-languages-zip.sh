#!/bin/sh
DIR='/tmp/languages'
mkdir -p $DIR
cp src/main/java/propertyFiles/* $DIR
cd $DIR
# exclude weird macOS files
zip -r languages.zip *.properties -x '**/.*' -x '**/__MACOSX'
