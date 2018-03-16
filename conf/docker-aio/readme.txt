first pass docker all-in-one image, intended for running integration tests against.

Could be potentially usable for normal development as well.


Initial setup (aka - do once):
- Do surgery on glassfish4 and solr7.2.1 following guides, place results in `conf/docker-aio/dv/deps` as `glassfish4dv.tgz` and `solr-7.2.1dv.tgz` respectively. If you `cd conf/docker-aio` and run `./0prep_deps.sh` these tarballs will be constructed for you.

Per-build:
- `cd conf/docker-aio`, and run `1prep.sh` to copy files for integration test data into docker build context; `1prep.sh` will also build the war file and installation zip file
- build the docker image: `docker build -t dv0 -f c7.dockerfile .`

- Run image: `docker run -d -p 8083:8080 --name dv dv0` (aka - forward port 8083 locally to 8080 in the container)

Note: If you see an error like this... `docker: Error response from daemon: Conflict. The container name "/dv" is already in use by container "5f72a45b68c86c7b0f4305b83ce7d663020329ea4e30fa2a3ce9ddb05223533d". You have to remove (or rename) that container to be able to reuse that name.` ... run something like `docker ps -a | grep dv` to see the container left over from the last run and something like `docker rm 5f72a45b68c8` to remove it. Then try the `docker run` command above again.

- Installation (integration test): `docker exec -it dv /opt/dv/setupIT.bash`

(Note that it's possible to customize the installation by editing `conf/docker-aio/default.config` and running `docker exec -it dv /opt/dv/install.bash` but for the purposes of integration testing, the `setupIT.bash` script above works fine.)

- update `dataverse.siteUrl` (appears only necessary for `DatasetsIT.testPrivateUrl`): `docker exec -it dv /usr/local/glassfish4/bin/asadmin create-jvm-options "-Ddataverse.siteUrl=http\://localhost\:8083"`

Run integration tests: 

First, cd back to the root of the repo where the `pom.xml` file is (`cd ../..` assuming you're still in the `conf/docker-aio` directory). Then run the test suite with script below:

`conf/docker-aio/run-test-suite.sh`

There isn't any strict requirement on the local port (8083 in this doc), the name of the image (dv0) or container (dv), these can be changed as desired as long as they are consistent.

