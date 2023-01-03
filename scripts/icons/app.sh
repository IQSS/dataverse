#!/bin/sh
# Filenames change each time so remove the old ones.
rm ../../src/main/webapp/resources/fontcustom/*
# Generate new files (see config file for paths).
fontcustom compile -c fontcustom-app.yml
# We want a fresh run each time since we have two scripts.
rm .fontcustom-manifest.json
# Use absolute src url path to "/resources..." 
# We've always done it this way.
# The sed command is extra verbose for precision and clarity.
sed -i -e 's/url("\.\./url("\/resources/' ../../src/main/webapp/resources/css/fontcustom.css
echo "Done! If all went well, you can see the icons at preview/fontcustom-preview.html"
