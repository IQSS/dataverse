#!/bin/sh
# Filenames change each time so remove the old ones.
rm ../../doc/sphinx-guides/source/_static/fontcustom/*
# Generate new files (see config file for paths).
fontcustom compile -c fontcustom-docs.yml
# We want a fresh run each time since we have two scripts.
rm .fontcustom-manifest.json
