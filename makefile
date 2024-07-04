
SPHINX_VERSION = $(shell grep "Sphinx" ./doc/sphinx-guides/requirements.txt | awk -F'==' '{print $$2}')
docs-html:
	docker run -it --rm -v $$(pwd):/docs sphinxdoc/sphinx:$(SPHINX_VERSION) bash -c "cd doc/sphinx-guides && pip3 install -r requirements.txt && make clean && make html"

docs-pdf:
	docker run -it --rm -v $$(pwd):/docs sphinxdoc/sphinx-latexpdf:$(SPHINX_VERSION) bash -c "cd doc/sphinx-guides && pip3 install -r requirements.txt && make clean && make latexpdf LATEXMKOPTS=\"-interaction=nonstopmode\"; cd ../.. && ls -1 doc/sphinx-guides/build/latex/Dataverse.pdf"

docs-epub:
	docker run -it --rm -v $$(pwd):/docs sphinxdoc/sphinx:$(SPHINX_VERSION) bash -c "cd doc/sphinx-guides && pip3 install -r requirements.txt && make clean && make epub"

docs-all:
	docker run -it --rm -v $$(pwd):/docs sphinxdoc/sphinx:$(SPHINX_VERSION) bash -c "cd doc/sphinx-guides && pip3 install -r requirements.txt && make clean && make html && make epub"
	docker run -it --rm -v $$(pwd):/docs sphinxdoc/sphinx-latexpdf:$(SPHINX_VERSION) bash -c "cd doc/sphinx-guides && pip3 install -r requirements.txt && make latexpdf LATEXMKOPTS=\"-interaction=nonstopmode\"; cd ../.. && ls -1 doc/sphinx-guides/build/latex/Dataverse.pdf"