
docs-html:
	docker run -it --rm -v $(shell pwd):/docs sphinxdoc/sphinx:7.3.6 bash -c "cd doc/sphinx-guides && pip3 install -r requirements.txt && make clean && make html"

docs-pdf:
	docker run -it --rm -v $(shell pwd):/docs sphinxdoc/sphinx:7.3.6 bash -c "cd doc/sphinx-guides && pip3 install -r requirements.txt && make clean && make latexpdf"

docs-epub:
	docker run -it --rm -v $(shell pwd):/docs sphinxdoc/sphinx:7.3.6 bash -c "cd doc/sphinx-guides && pip3 install -r requirements.txt && make clean && make epub"

docs-all:
	docker run -it --rm -v $(shell pwd):/docs sphinxdoc/sphinx:7.3.6 bash -c "cd doc/sphinx-guides && pip3 install -r requirements.txt && make clean && make html && make epub && make latexpdf"