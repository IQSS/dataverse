# About these instructions

The purpose of this document is to provide instruction on how to build a fresh copy of the Dataverse Sphinx documentation. You will focus on using (Docker scripts)[https://www.docker.com/] to setup the build environment. If you need help with Sphinx, visit [https://www.sphinx-doc.org].

The following instructions were written for a bash in a Linux environment (WSL terminal on Windows 11 machine), but should apply to most unix environments.

## Configuring your environment variables

To simplify the instructions you need to create environment variables for your environment.
Your root Dataverse sphinx-guide path will be set using a `ROOT_DATAVERSE_SG` variable:
`export ROOT_DATAVERSE_SG="/mnt/q/GitHubRepos/kuhlaid/dataverse/doc/sphinx-guides"`

Next create a variable `DKR_DV_PDF` to store the unique name for the Docker image being created to run the PDF Sphinx builds.
`export DKR_DV_PDF="sddi_pdf"`

Next create a variable `DKR_DV_HTML` to store the unique name for the Docker image being created to run the HTML Sphinx builds.
`export DKR_DV_HTML="sddi_html"`

This next variable `DKR_DV_HTML_VIEW` is on used if you wish to test the HTML documentation in a local Apache container within Docker.
`export DKR_DV_HTML_VIEW="sddi_html_view"`

## PDF build scripts using Docker

First you will generate the PDF version of the Dataverse documentation. The reason for this is HTML documentation creates a link to this PDF file. Below are the bash commands to build a Docker image using Sphinx and Latex.

```s (bash)
# change to the `SphinxDocBuildPDF` directory
cd $ROOT_DATAVERSE_SG/SphinxDocBuildPDF
# build the Docker image from the Dockerfile script
docker build -t $DKR_DV_PDF .
# change back to the root Dataverse documentation directory
cd $ROOT_DATAVERSE_SG
# create the PDF version of the Dataverse documentation (since you need this for the HTML docs)
docker run --rm -v $ROOT_DATAVERSE_SG:/docs $DKR_DV_PDF make latexpdf
# copy the freshly built PDF file to the source folder under static files (your HTML build will be looking for this file)
cp $ROOT_DATAVERSE_SG/build/latex/Dataverse.pdf "$ROOT_DATAVERSE_SG/source/_static"
```

## HTML build scripts using Docker

Next you will generate the Docker image for building the HTML version of the Dataverse documentation. Below are the bash commands to build a Docker image using Sphinx:

```s (bash)
cd $ROOT_DATAVERSE_SG/SphinxDocBuildHtml
docker build -t $DKR_DV_HTML .
```

Lastly, you can make the Dataverse Sphinx documentation. ***Note the `/[project directory]:/docs` command below gives the impression that a `docs` folder should exist, but this is just standard Sphinx syntax and Sphinx will look through the `source` directory.***

```s (bash)
cd $ROOT_DATAVERSE_SG
## remove the existing docker image for HTML processing if needed
# docker image rm -f $DKR_DV_HTML
## if you are rerunning the HTML build then you need to remove the /build/html directory so it can be recreated
# rm -r $ROOT_DATAVERSE_SG/build/html
docker run --rm -v $ROOT_DATAVERSE_SG:/docs $DKR_DV_HTML make html
```

To see the documentation build, simply open the `build\html\index.html` file in your web browser.

```s (bash)
# copy the `SphinxDocLocalHtmlImage/Dockerfile` to the build directory if you are wanting to run a localhost example of the generated documentation (since Docker is only able to 'look within/below' the current directory of the Dockerfile)
cp $ROOT_DATAVERSE_SG/SphinxDocLocalHtmlImage/Dockerfile $ROOT_DATAVERSE_SG/build
# you need to copy the font awesome font files to the html build directory since the `sphinxcontrib.icon` module is not including them
cp -r $ROOT_DATAVERSE_SG/source/_font $ROOT_DATAVERSE_SG/build/html
# change directories to the Sphinx build
cd $ROOT_DATAVERSE_SG/build
# create a Docker static documents image with a copy of the freshly built Sphinx docs
docker build -t $DKR_DV_HTML_VIEW .
# start an Apache container running the static documents image
docker run --publish 80:80 --detach --name localhost_sddi $DKR_DV_HTML_VIEW
# visit http://localhost/index.html in a browser to test the HTML documentation
```

## Issues with PDF output

**DO NOT nest HTML/documentation lists more than three deep (see the issue regarding this on GitHub at [https://github.com/IQSS/dataverse/issues/9277]).**

If you see errors when building the PDF you can copy the `Dataverse.tex` file contents under the `/build/latex` directory into a LaTeX checker such as [https://www.dainiak.com/latexcheck], but if you run into problems such as the nested documentation lists then the errors can be unhelpful (but likely  the documentation file appearing in the error is causing problems in some way).

### Check the code of the Dataverse.tex file using https://www.dainiak.com/latexcheck/.
- one of the common problems is non-ASCII text being used (such as The character U+2019 "’" could be confused with the character U+0060 "`", which is more common in source code)
- also do not include emojis in the documentation
- If you would like to search for possible problematic characters run `LC_ALL=C find . -type f -exec grep -c -P -n "[^\x00-\x7F]" {} + ` within the source directory (any files with non-ASCII characters will have a number to the right greater than zero). If the `./developers/dependencies.rst` file happens to have any non-ASCII characters then you can check the location of the characters using `LC_ALL=C grep --color='auto' -P -n "[\x80-\xFF]" ./developers/dependencies.rst`. Note: not all non-ASCII characters are problematic.

## If you are new to Sphinx then you can use the following Docker command to create a starter Sphinx environment

You use the Docker image you just built to create the Sphinx project template:

```s (bash)
docker run -it --rm -v $ROOT_DATAVERSE_SG:/docs $DKR_DV_HTML sphinx-quickstart
```

At this point you have a boilerplate `source` folder with some `hello world` documentation. You can copy the dataverse documentation source from the `dataverse\doc\sphinx-guides\source` GitHub directory and replace the boiler plate source directory Sphinx just created for us.
