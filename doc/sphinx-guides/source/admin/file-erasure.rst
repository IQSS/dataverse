File Erasure
============

.. contents:: Contents:
	:local:

Overview
--------

Dataverse has functionality to erase the physical bytes represented by a dataset datafile. This capability can be used independently or in conjunction with deletion of datafiles from dataset versioning and dataset version deaccessioning. It is intended to support removal of files after their retention period is over or in response to take-down requests such as those allowed by the European General Data Protection Regulation (GDPR).

Note that conformance with regulations such as GDPR or the legal requirements related to file retention will involve appropriate policies, procedures, and configuration of repository infrastructure and storage that are not addressed by Dataverse's capabilities. This guide page describes Dataverse's functionality and provides some limited, best-effort, guidance related to developing appropriate take-down/retention capabilities.

When a File Erasure request is processed, Dataverse triggers a standard operating system file deletion command via Java (``java.nio.file.Files.delete``).
This operation instantly removes the file pointer, rendering the data inaccessible to the operating system and application layers. However, whether the underlying physical blocks are immediately or permanently scrubbed depends entirely on your specific hardware and storage configuration.

Erasure applies only to files that have been published (at least one released dataset version contains the file). Draft-only files cannot be erased; use the standard file delete API instead. Erasure of harvested files is not supported.

To ensure full compliance with data protection regulations, system administrators must ensure their host infrastructure adheres to one of the following compliant deployment patterns.

Pattern A: Solid-State Drives (SSDs) & NVMe Storage
---------------------------------------------------

Modern flash storage uses a Flash Translation Layer (FTL) and wear leveling. When a file is deleted, the physical data blocks remain intact until a drive maintenance cycle clears them.

**Mandatory OS Configuration:** Ensure that TRIM (Linux/macOS) or Defrag/Optimize Drives (Windows) is enabled and scheduled to run aggressively.

- **Linux:** Verify the ``fstrim.timer`` systemd service is active (weekly is standard, but daily may be preferred depending on your take-down SLA).
- **Windows:** Ensure the "Optimize Drives" scheduled task is running.

**Why this matters:** TRIM explicitly signals to the SSD controller that the deleted file's blocks are no longer needed. The drive's internal garbage collection will then permanently erase the physical flash cells during idle time, ensuring true physical sanitization.

Pattern B: Traditional Hard Disk Drives (HDDs) & SAN/NAS Arrays
---------------------------------------------------------------

Spinning disks do not automatically clear data blocks upon file deletion; they simply mark the space as available for future writes.

**Mandatory OS Configuration:** If deploying on traditional mechanical disks, the hosting filesystem must be configured to securely clear unallocated space automatically.

- **Linux (ext4):** Ensure the filesystem is mounted with the ``discard`` option if supported by the underlying storage array, or schedule automated secure free-space wiping utilities (such as ``sfill`` from the ``secure-delete`` package) during off-peak hours.
- **Windows:** Implement a regular scheduled task using ``cipher /w:X:`` (where ``X`` is the drive letter) to overwrite deactivated blocks and deleted data across the volume's free space.

Pattern C: Cloud Block Storage (AWS EBS, Azure Managed Disks, GCP Persistent Disks)
-----------------------------------------------------------------------------------

If hosting the application in a cloud environment, block storage devices are abstracted and multi-tenant.

**Cloud Best Practices:** Administrators should rely on the cloud provider's native hardware isolation.

**Data at Rest Encryption:** Ensure that the underlying cloud volume is fully encrypted at rest using provider-managed or customer-managed keys (e.g., AWS KMS, Azure Key Vault).

**Why this matters:** Cloud providers guarantee that when an encrypted volume or block is unallocated or deleted, the underlying physical infrastructure is sanitized or the specific block cryptographic keys are destroyed before hardware blocks are reassigned to other tenants.


Pattern D: Amazon S3 & Object Storage (AWS S3, Azure Blob, MinIO)
-----------------------------------------------------------------

If the application is configured to store large files directly in Amazon S3 or an S3-compatible object storage repository, a standard application deletion command only removes the current object pointer. If features like Versioning or Object Locking are enabled on the bucket, the physical data blocks will remain permanently accessible unless explicit lifecycle controls are enforced by the cloud administrator.
• Mandatory S3 Lifecycle Configuration: Administrators must configure an automated S3 Lifecycle Policy on the target bucket to guarantee permanent, unrecoverable erasure of all historical iterations and deleted files.
• Noncurrent Version Expiration: If Bucket Versioning is active, deleting a file from the Java application creates a "Delete Marker" and moves the actual file to a "Noncurrent Version" state. To ensure full compliance, configure a lifecycle rule to permanently expire and permanently remove noncurrent versions after a strict compliance buffer window (e.g., 7 to 14 days).
• Incomplete Multipart Upload Cleanups: Large files uploaded via multipart streams can leave orphaned, hidden data fragments if a network drop or timeout occurs mid-upload. Configure a rule to automatically abort and delete incomplete multipart uploads after 7 days to prevent residual data persistence.
• Handling Object Lock & Compliance Mode: If your bucket utilizes S3 Object Lock in "Compliance Mode" for Write-Once-Read-Many (WORM) requirements, even system administrators cannot bypass retention dates. Ensure that the retention window configured on the bucket does not legally conflict with your organization’s strict GDPR erasure SLAs.

Example: Compliant AWS S3 Lifecycle Policy (JSON)

Administrators can apply this lifecycle configuration shell directly to the asset bucket via the AWS CLI or Terraform:
json
{
  "Rules": [
    {
      "ID": "GDPR-Compliant-Erasure-Policy",
      "Status": "Enabled",
      "Filter": {
        "Prefix": ""
      },
      "NoncurrentVersionExpiration": {
        "NoncurrentDays": 14
      },
      "AbortIncompleteMultipartUpload": {
        "DaysAfterInitiation": 7
      }
    }
  ]
}
Use code with caution.
• Data at Rest Encryption (SSE-S3 / SSE-KMS): Ensure the bucket has default server-side encryption enabled. When objects or their underlying versions are permanently deleted via lifecycle rules, AWS automatically sanitizes the storage blocks and updates the internal block allocation, ensuring the data can never be read by another cloud tenant.

Critical Requirement: Backup & Snapshot Lifecycle Policy
--------------------------------------------------------

Data protection authorities universally accept that backup media cannot always be cleared instantaneously upon a single user deletion request. However, to remain compliant:

1. **Declared Retention Window:** Organizations must define a strict backup retention policy (e.g., 30, 60, or 90 days).
2. **Immutable Expiry:** Backups, VM snapshots, database mirrors, and archival copies containing the erased file must be completely overwritten or destroyed automatically once they age out of the retention window.
3. **Restoration Protocol:** If a historical backup must be restored to production, administrators must re-run a compliance synchronization script to re-delete files for any users who requested erasure during the intervening period.

API Reference
-------------

Erasing a file is performed via the REST API by superusers only. The operation uses a two-step confirmation flow to prevent accidental erasure.

Erasure Endpoint
~~~~~~~~~~~~~~~~

.. code-block:: bash

   DELETE /api/files/{id}/erase

The ``{id}`` parameter accepts either a numeric file ID or a persistent identifier (for example, ``doi:10.5072/FK2...``).

Request Body
~~~~~~~~~~~~

The request body is JSON with the following fields:

- **reason** (required, string): A free-text explanation of why the file is being erased (for example, a GDPR request reference number).
- **confirmationToken** (required on the second step, string): The token returned by the first step.
- **manuallyErased** (optional, boolean): Set to ``true`` when confirming erasure of a file on a storage location Dataverse cannot write to directly.

Two-Step Confirmation Flow
~~~~~~~~~~~~~~~~~~~~~~~~~~

To prevent accidental erasure, the API requires two requests:

**Step 1 — Request a confirmation token:**

.. code-block:: bash

   curl -H "X-Dataverse-key: $API_TOKEN" \
     -X DELETE \
     -d '{"reason": "GDPR request #12345"}' \
     "$SERVER_URL/api/files/$FILE_ID/erase"

The server validates that the file is published and not harvested, then returns a ``202 Accepted`` response with a confirmation token valid for 5 minutes:

.. code-block:: json

   {
     "status": "confirmation_required",
     "confirmationToken": "a1b2c3d4-...",
     "tokenExpiresInSeconds": 300,
     "fileId": 42,
     "persistentId": "doi:10.5072/FK2...",
     "message": "To complete the erasure, send another DELETE request with the confirmationToken included."
   }

If the file is stored on a location Dataverse cannot write to (for example, a Globus endpoint or a remote overlay store), the message field indicates that manual deletion is required:

.. code-block:: json

   {
     "status": "confirmation_required",
     "confirmationToken": "a1b2c3d4-...",
     "tokenExpiresInSeconds": 300,
     "fileId": 42,
     "message": "This file is stored on a location not directly accessible by Dataverse. Please delete the main file manually, then send another DELETE request with both the confirmationToken and manuallyErased=true to complete the erasure."
   }

**Step 2 — Confirm the erasure:**

For files on storage Dataverse can write to directly (local filesystem, S3, Swift):

.. code-block:: bash

   curl -H "X-Dataverse-key: $API_TOKEN" \
     -X DELETE \
     -d '{"reason": "GDPR request #12345", "confirmationToken": "a1b2c3d4-..."}' \
     "$SERVER_URL/api/files/$FILE_ID/erase"

For files on storage Dataverse cannot write to, after manually deleting the file from the remote location:

.. code-block:: bash

   curl -H "X-Dataverse-key: $API_TOKEN" \
     -X DELETE \
     -d '{"reason": "GDPR request #12345", "confirmationToken": "a1b2c3d4-...", "manuallyErased": true}' \
     "$SERVER_URL/api/files/$FILE_ID/erase"

A successful erasure returns a ``200 OK`` response:

.. code-block:: json

   {
     "status": "erased",
     "fileId": 42,
     "persistentId": "doi:10.5072/FK2..."
   }

If the file is already erased, the endpoint returns ``410 Gone``.

Storage Read and Write Capability Checks
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Dataverse determines whether it can erase a file directly by checking the storage driver's write capability. For local filesystem, S3, and Swift stores, Dataverse deletes both the main file and all auxiliary files (thumbnails, cached tabular data, preserved originals, and search index files) automatically.

For stores Dataverse cannot write to (such as Globus endpoints or remote overlay stores), Dataverse deletes only cached auxiliary files. The main data file must be deleted manually by the administrator. The API response in Step 1 indicates when manual deletion is required.

When the administrator confirms with ``manuallyErased: true``, Dataverse performs a verification step if the store is readable: it checks whether the main file still exists in storage. If the file is still present, the erasure fails with an error. If the store is not readable by Dataverse either, the confirmation proceeds based on the administrator's assertion alone.

Audit Record
~~~~~~~~~~~~

Each erasure creates a ``FileErasure`` database record linked to the erased ``DataFile``. The record retains:

- **erasedBy**: The superuser who performed the erasure.
- **erasedOn**: The timestamp of the erasure.
- **reason**: The free-text reason provided in the API request.
- **manuallyDeleted**: A boolean flag indicating whether the main file was deleted manually by the administrator (``true``) or directly by Dataverse (``false``). This flag provides a permanent audit trail distinguishing erasures that involved manual intervention from those handled entirely by Dataverse.

The ``DataFile`` database record is retained with a reference to the ``FileErasure`` record. The storage identifier is preserved (not cleared) so that the original file location can be manually verified after erasure.

Download and Access Blocking
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

After erasure, attempts to download the file return HTTP ``410 Gone``. This applies to:

- Single-file downloads via ``GET /api/access/datafile/{id}``.
- Bulk or bundle downloads that include an erased file.
- Access requests via ``POST /api/access/datafile/{id}/requestAccess``.

Draft Version Handling
~~~~~~~~~~~~~~~~~~~~~~

If a draft (working copy) version exists when a file is erased, the file's metadata entry is removed from that draft. If no draft exists, no new draft is created. To fully remove the file from published history, publish a new version without the erased file and deaccession earlier versions that still reference it.

Error Responses
~~~~~~~~~~~~~~~

| **400 Bad Request**: Missing or invalid ``reason``; expired or invalid confirmation token; attempt to erase a draft-only file; attempt to erase a harvested file; or attempt to confirm erasure of a file on an inaccessible store without ``manuallyErased: true``.
| **403 Forbidden**: The requesting user is not a superuser.
| **410 Gone**: The file has already been erased.
