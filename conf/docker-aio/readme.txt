first pass docker all-in-one image, intended for running integration tests against.

Could be potentially usable for normal development as well.


Initial setup (aka - do once):
- Do surgery on glassfish4 and solr4.6.0 following guides, place results in `conf/docker-aio/dv/deps` as `glassfish4dv.tgz` and `solr-4.6.0dv.tgz` respectively. Running `conf/docker-aio/0prep_deps.sh` attempts to automate this.

Per-build:
- `cd conf/docker-aio`, and run `1prep.sh` to copy files for integration test data into docker build context; `1prep.sh` will also build the war file and installation zip file
- build the docker image: `docker build -t dv0 -f c7.dockerfile .`

- Run image: `docker run -d -p 8083:8080 --name dv dv0` (aka - forward port 8083 locally to 8080 in the container)
- Installation (integration test): `docker exec -it dv /opt/dv/setupIT.bash`
- Installation (non-interactive, uses `conf/docker-aio/default.config`): `docker exec -it dv /opt/dv/install.bash`

- update `dataverse.siteUrl` (appears only necessary for `DatasetsIT.testPrivateUrl`): `docker exec -it dv /usr/local/glassfish4/bin/asadmin create-jvm-options "-Ddataverse.siteUrl=http\://localhost\:8083"`

Run integration tests: 
`mvn test -Dtest=DataversesIT,DatasetsIT,SwordIT,AdminIT,BuiltinUsersIT,UsersIT,UtilIT,ConfirmEmailIT,FileMetadataIT -Ddataverse.test.baseurl='http://localhost:8083'`

There isn't any strict requirement on the local port (8083 in this doc), the name of the image (dv0) or container (dv), these can be changed as desired as long as they are consistent.

