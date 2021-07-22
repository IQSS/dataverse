TODO: Move some or all into dev guide.

Ruby 3.x doesn't seem to work with FontCustom. Ruby 2.7.2 does.

`brew install sfnt2woff` isn't mentioned in the FontCustom README but it's in mentioned in https://github.com/FontCustom/fontcustom/pull/385

If you want to create a zip for UX folks to preview, you can run a webserver (`python3 -m http.server`) and grab all the files with this:

wget -m -p -E -k http://localhost:8000/scripts/icons/preview/fontcustom-preview.html
