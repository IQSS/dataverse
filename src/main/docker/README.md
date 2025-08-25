# Dataverse Application Container Image

The "application image" offers you a deployment-ready Dataverse application running on the underlying
application server, which is provided by the [base image](https://hub.docker.com/r/gdcc/base). 
Its sole purpose is to bundle the application and any additional material necessary to successfully jumpstart
the application.

Note: Until all :ref:`jvm-options` are *MicroProfile Config* enabled, it also adds the necessary scripting glue to
configure the applications domain during booting the application server. See :ref:`app-tunables`.

## Quick Reference

**Maintained by:**

This image is created, maintained and supported by the Dataverse community on a best-effort basis.

**Where to find documentation:**

The [Dataverse Container Guide - Application Image](https://guides.dataverse.org/en/latest/container/app-image.html)
provides in-depth information about content, building, tuning and so on for this image. You should also consult
the [Dataverse Container Guide - Base Image](https://guides.dataverse.org/en/latest/container/base-image.html) page
for more details on tunable settings, locations, etc.

**Where to get help and ask questions:**

IQSS will not offer support on how to deploy or run it. Please reach out to the community for help on using it.
You can join the Community Chat on Matrix at https://chat.dataverse.org and https://groups.google.com/g/dataverse-community
to ask for help and guidance.

## Supported Image Tags

This image is sourced within the main upstream code [repository of the Dataverse software](https://github.com/IQSS/dataverse).
Development and maintenance of the [image's code](https://github.com/IQSS/dataverse/tree/develop/src/main/docker) happens there (again, by the community).

Our tagging is inspired by [Bitnami](https://docs.vmware.com/en/VMware-Tanzu-Application-Catalog/services/tutorials/GUID-understand-rolling-tags-containers-index.html).
For more detailed information about our tagging policy, please read about our [application image tags](https://guides.dataverse.org/en/latest/container/app-image.html#supported-image-tags) in the Dataverse Containers Guide.

For ease of use, here is a list of images that are currently maintained.

<!-- TAG BLOCK HERE -->

All of them are rolling tags, except those ending with `-r<number>`, which are the most recent immutable tags.
The `unstable` tags are the current development branch snapshot.
We strongly recommend using only immutable tags for production use cases.

Within the main repository, you may find the application image files at `<git root>/src/main/docker`.
This Maven module uses the [Maven Docker Plugin](https://dmp.fabric8.io) to build and ship the image.
You may use, extend, or alter this image to your liking and/or host in some different registry if you want to.

**Supported architectures:** This image is created as a "multi-arch image", supporting the most common architectures
Dataverse usually runs on: AMD64 (Windows/Linux/...) and ARM64 (Apple M1/M2).

## License

Image content created by the community is licensed under [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0),
like the [main Dataverse project](https://github.com/IQSS/dataverse/blob/develop/LICENSE.md).

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and limitations under the License.

As with all Docker images, all images likely also contain other software which may be under other licenses (such as
[Payara Server](https://github.com/payara/Payara/blob/master/LICENSE.txt), Bash, etc., from the base
distribution, along with any direct or indirect (Java) dependencies contained).

As for any pre-built image usage, it is the image user's responsibility to ensure that any use of this image complies
with any relevant licenses for all software contained within.
