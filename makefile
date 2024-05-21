all::
	PWD := $(shell pwd)
	
guides:
	docker run -it --rm -v $(PWD):/docs sphinxdoc/sphinx:7.3.6 bash -c "cd doc/sphinx-guides && pip3 install -r requirements.txt && make clean && make html"