## New Globus Features in rdm-integration 2.0.1

[rdm-integration](https://github.com/libis/rdm-integration) is a Dataverse external tool for synchronizing files from various source repositories into Dataverse, with support for background processing, DDI-CDI metadata generation, and high-performance Globus transfers.

Release 2.0.1 brings several new Globus capabilities:

- **Guest downloads** — public datasets can be downloaded via Globus without a Dataverse account
- **Preview URL support** — reviewers can download draft dataset files via Globus using general preview URLs
- **Scoped institutional login** — `session_required_single_domain` support enables access to institutional Globus endpoints (e.g., HPC clusters); scopes are automatically removed for guest and preview access
- **Real-time transfer progress** — polling-based progress monitoring with percentage display and status updates (ACTIVE/SUCCEEDED/FAILED)
- **Download filtering** — only datasets where the user can download all files are shown, avoiding failed transfers for restricted or embargoed content
- **Hierarchical file tree** — recursive folder selection and color-coded file status

For full details, see the [README](https://github.com/libis/rdm-integration#readme) and [GLOBUS_INTEGRATION.md](https://github.com/libis/rdm-integration/blob/main/GLOBUS_INTEGRATION.md).

