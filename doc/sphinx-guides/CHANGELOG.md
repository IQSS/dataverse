# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [future] - 2023.01.17

- [x] updated `\doc\sphinx-guides\source\container\base-image.rst` with footnote numbers instead of letters since footnotes with letters will cause the PDF build to include the footnotes in a Biography section of the PDF
- [x] preventing LaTeX from including an `Indices and Table` section in the generated PDF with only a single line of text reading `search` (needed to change the root `index.rst` file to not have this section at the end)
- [x] submit pull request `9277 - Correcting the Sphinx documentation PDF build` 
- [x] creating `.github\workflows\action-sphinx-doc-build-pdf.yml` action script to create the Docker image for building the PDF document (commit [https://github.com/kuhlaid/dataverse/commit/24f564f7bee8200147134f49408888d474f392cf] successfully built the Docker image)

## To do items

- [ ] create a GitHub workflow for checking the documentation using something like [https://github.com/OdumInstitute/sphinx-action] or [https://docs.github.com/en/actions/creating-actions/creating-a-docker-container-action#testing-out-your-action-in-a-workflow]
- [ ] determine the `\doc\sphinx-guides\source\img` files no longer in use and determine if they should be added back in and need to be updated to reflect UI changes
- [ ] long tables do not split across pages in LaTeX, such as the `#datasetField (field) properties` table in `\doc\sphinx-guides\source\admin\metadatacustomization.rst`, so the table simply truncates at the page break (see if this is fixed when the table is generate using csv table format)
- [x] switch inline tables such as the ones on `\doc\sphinx-guides\source\admin\metadatacustomization.rst` to use csv tables such as those in `\doc\sphinx-guides\source\api\external-tools.rst` because formatting tables manually is a nightmare (search for instances of `+---` in the docs); long tables (that extend beyond one page MUST have a `:class: longtable` defined for the CSV table otherwise the table will truncate at the end of the first page)
- [ ] include warning instructions for those wanting to contribute to the documentation that csv tables should be used over manually formatted
- [x] replace instances of `guides.dataverse.org/en` with :ref references (since we do not want any hard coded domain name paths when not necessary)
- [ ] might want to create a `docker-compose.yml` using [https://docs.docker.com/compose/gettingstarted/#step-3-define-services-in-a-compose-file] to handle the different steps/images instead of using the markdown file to step through things manually
- [ ] see if the dvinstall process is different than described in `https://guides.dataverse.org/en/latest/installation/prerequisites.html?highlight=dvinstall`
- [ ] why is psycopg2 not part of the install prerequisites? `https://guides.dataverse.org/en/latest/developers/dev-environment.html?highlight=psycopg2`
- [ ] the documentation needs to set the shell type when running terminal commands because some assume `sh` and others assume `bash`

## Issues

## Things to consider when updating the documentation

- when making changes to the docs (especially if adding a long table), a spot check of the PDF is a good idea since tables can be tricky to format