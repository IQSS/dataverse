# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [future] - 2023.01.17

- [x] updated `\doc\sphinx-guides\source\container\base-image.rst` with footnote numbers instead of letters since footnotes with letters will cause the PDF build to include the footnotes in a Biography section of the PDF
- [x] preventing LaTeX from including an `Indices and Table` section in the generated PDF with only a single line of text reading `search` (needed to change the root `index.rst` file to not have this section at the end)

## To do items

- [ ] determine the `\doc\sphinx-guides\source\img` files no longer in use and determine if they should be added back in and need to be updated to reflect UI changes
- [ ] long tables do not split across pages in LaTeX, such as the `#datasetField (field) properties` table in `\doc\sphinx-guides\source\admin\metadatacustomization.rst`, so the table simply truncates at the page break (see if this is fixed when the table is generate using csv table format)
- [ ] switch inline tables such as the ones on `\doc\sphinx-guides\source\admin\metadatacustomization.rst` to use csv tables such as those in `\doc\sphinx-guides\source\api\external-tools.rst` because formatting tables manually is a nightmare
- [ ] include warning instructions for those wanting to contribute to the documentation that csv tables should be used over manually formatted