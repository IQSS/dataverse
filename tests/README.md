See doc/sphinx-guides/source/developers/testing.rst

To run the metadata block properties verifier in Docker, use:

```bash
tests/verify_mdb_properties_docker.sh
```

This builds a local `dataverse-verify-mdb-properties` image with GraalVM native-image
and JBang, then runs `tests/verify_mdb_properties.sh` inside the container.
