# Docker All-In-One

First pass docker all-in-one image, intended for running integration tests against.
Also usable for normal development and system evaluation; not intended for production.

### Requirements:
 - java11 compiler, maven, make, wget, docker

### Quickstart:
 - in the root of the repository, run `./conf/docker-aio/prep_it.bash`
 - if using DataCite test credentials, update the build args appropriately.
 - if all goes well, you should see the results of the `api/info/version` endpoint, including the deployed build (eg `{"status":"OK","data":{"version":"4.8.6","build":"develop-c3e9f40"}}`). If not, you may need to read the non-quickstart instructions.
 - run integration tests: `./conf/docker-aio/run-test-suite.sh`

----

## More in-depth documentation:


### Initial setup (aka - do once):
- `cd conf/docker-aio` and run `./0prep_deps.sh` to created Payara and Solr tarballs in `conf/docker-aio/dv/deps`.

### Per-build:

> Note: If you encounter any issues, see the Troubleshooting section at the end of this document.

#### Setup

- `cd conf/docker-aio`, and run `./1prep.sh` to copy files for integration test data into docker build context; `1prep.sh` will also build the war file and installation zip file
- build the docker image: `docker build -t dv0 -f c8.dockerfile .`

- Run image: `docker run -d -p 8083:8080 -p 8084:80 --name dv dv0` (aka - forward port 8083 locally to 8080 in the container for payara, and 8084 to 80 for apache); if you'd like to connect a java debugger to payara, use `docker run -d -p 8083:8080 -p 8084:80 -p 9010:9009 --name dv dv0`

- Installation (integration test): `docker exec dv /opt/dv/setupIT.bash` 
  (Note that it's possible to customize the installation by editing `conf/docker-aio/default.config` and running `docker exec dv /opt/dv/install.bash` but for the purposes of integration testing, the `setupIT.bash` script above works fine.)

- update `dataverse.siteUrl` (appears only necessary for `DatasetsIT.testPrivateUrl`): `docker exec dv /usr/local/glassfish4/bin/asadmin create-jvm-options "-Ddataverse.siteUrl=http\://localhost\:8084"` (or use the provided `seturl.bash`)

#### Run integration tests: 

First, cd back to the root of the repo where the `pom.xml` file is (`cd ../..` assuming you're still in the `conf/docker-aio` directory). Then run the test suite with script below:

`conf/docker-aio/run-test-suite.sh`

There isn't any strict requirement on the local port (8083, 8084 in this doc), the name of the image (dv0) or container (dv), these can be changed as desired as long as they are consistent.

### Troubleshooting Notes:

* If Dataverse' build fails due to an error about `Module` being ambiguous, you might be using a Java 9 compiler.

* If you see an error like this: 
 ```
 docker: Error response from daemon: Conflict. The container name "/dv" is already in use by container "5f72a45b68c86c7b0f4305b83ce7d663020329ea4e30fa2a3ce9ddb05223533d"
 You have to remove (or rename) that container to be able to reuse that name.
 ``` 
    run something like `docker ps -a | grep dv` to see the container left over from the last run and something like `docker rm 5f72a45b68c8` to remove it. Then try the `docker run` command above again.

* `empty reply from server` or `Failed to connect to ::1: Cannot assign requested address` tend to indicate either that you haven't given payara enough time to start, or your docker setup is in an inconsistent state and should probably be restarted.

* For manually fiddling around with the created dataverse, use user `dataverseAdmin` with password `admin1`.
