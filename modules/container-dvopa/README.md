# dvopa - a Dataverse Kubernetes Operator

> **Warning**
This is experimental, work in progress and mostly a Proof Of Concept.
Expect breaking changes as well as broken functionality.
Use at your own peril.

This project aims at creating a [Kubernetes Operator](https://kubernetes.io/docs/concepts/extend-kubernetes/operator)
to deploy and maintain a cloud-based Dataverse installation.

To continue with puns: `dvopa` means `Dataverse Operator`, yet _opa_ references to the german nickname for
a grandfather. Like a guarding grandpa it will take care of children and grandchildren pods, will read fairy tales
to your cluster and books to your deployment.

## Technology

- Java 17 (`record`s FTW!)
- [Quarkus](https://quarkus.io)
- [Java Operator SDK](https://docs.quarkiverse.io/quarkus-operator-sdk/dev/)
- Maven and [Quarkus Maven Plugin](https://quarkus.io/guides/quarkus-maven-plugin)
- [JIB](https://github.com/GoogleContainerTools/jib) and [Quarkus JIB](https://quarkus.io/guides/container-image#jib)


# Images

Multiarch is supported when pushing to a registry.
Use `mvn install -Dquarkus.container-image.registry=ghcr.io -Dquarkus.container-image.push=true -Dquarkus.jib.platforms=linux/amd64,linux/arm64/v8` or likewise.
