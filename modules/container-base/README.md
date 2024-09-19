# Dataverse Base Container Image

The Dataverse Base Container Image contains primarily a pre-installed and pre-tuned application server with the
necessary software dependencies for deploying and launching a Dataverse repository installation.

Adding basic functionality like executing scripts at container boot, monitoring, memory tweaks, etc., is all done
at this layer. Application images building from this very base focus on adding deployable Dataverse code and 
actual scripts.

There is a community based [application image](https://hub.docker.com/r/gdcc/dataverse) 
([docs](https://guides.dataverse.org/en/latest/container/app-image.html)), but you may create your own or even reuse
this image for other purposes than the Dataverse application.

## Quick Reference

**Maintained by:** 

This image is created, maintained and supported by the Dataverse community on a best-effort basis.

**Where to find documentation:**

The [Dataverse Container Guide - Base Image](https://guides.dataverse.org/en/latest/container/base-image.html)
provides in-depth information about content, building, tuning and so on for this image. 

**Where to get help and ask questions:**

IQSS will not offer support on how to deploy or run it. Please reach out to the community for help on using it.
You can join the Community Chat on Matrix at https://chat.dataverse.org and https://groups.google.com/g/dataverse-community
to ask for help and guidance.

## Supported Image Tags

This image is sourced within the main upstream code [repository of the Dataverse software](https://github.com/IQSS/dataverse).
Development and maintenance of the [image's code](https://github.com/IQSS/dataverse/tree/develop/modules/container-base) happens there (again, by the community).
Community-supported image tags are based on the two most important branches:

Our tagging is inspired by [Bitnami](https://docs.vmware.com/en/VMware-Tanzu-Application-Catalog/services/tutorials/GUID-understand-rolling-tags-containers-index.html).
For more detailed information about our tagging policy, please read about our [base image tags](https://guides.dataverse.org/en/latest/container/base-image.html#supported-image-tags) in the Dataverse Containers Guide.

For ease of use, here is a list of images that are currently maintained.

<!-- TAG BLOCK HERE -->

All of them are rolling tags, except those ending with `-r<number>`, which are the most recent immutable tags.
The `unstable` tags are the current development branch snapshot.
We strongly recommend using only immutable tags for production use cases.

Within the main repository, you may find the base image files at `<git root>/modules/container-base`.
This Maven module uses the [Maven Docker Plugin](https://dmp.fabric8.io) to build and ship the image.
You may use, extend, or alter this image to your liking and/or host in some different registry if you want to under the terms of the Apache 2.0 license.

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
