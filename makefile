
SPHINX_VERSION = $(shell grep "Sphinx" ./doc/sphinx-guides/requirements.txt | awk -F'==' '{print $$2}')
docs-html:
	docker run -it --rm -v $(shell pwd):/docs sphinxdoc/sphinx:$(SPHINX_VERSION) bash -c "cd doc/sphinx-guides && pip3 install -r requirements.txt && make clean && make html"

docs-pdf:
	docker run -it --rm -v $(shell pwd):/docs sphinxdoc/sphinx:$(SPHINX_VERSION) bash -c "cd doc/sphinx-guides && pip3 install -r requirements.txt && make clean && make latexpdf"

docs-epub:
	docker run -it --rm -v $(shell pwd):/docs sphinxdoc/sphinx:$(SPHINX_VERSION) bash -c "cd doc/sphinx-guides && pip3 install -r requirements.txt && make clean && make epub"

docs-all:
	docker run -it --rm -v $(shell pwd):/docs sphinxdoc/sphinx:$(SPHINX_VERSION) bash -c "cd doc/sphinx-guides && pip3 install -r requirements.txt && make clean && make html && make epub && make latexpdf"