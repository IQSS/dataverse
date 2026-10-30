File Erasure
============

.. contents:: Contents:
	:local:

Overview
--------

When a "Right to Erasure" (GDPR) request is processed, this application triggers a standard operating system file deletion command via Java (``java.nio.file.Files.delete``).
This operation instantly removes the file pointer, rendering the data inaccessible to the operating system and application layers. However, whether the underlying physical blocks are immediately or permanently scrubbed depends entirely on your specific hardware and storage configuration.

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

Critical Requirement: Backup & Snapshot Lifecycle Policy
--------------------------------------------------------

Data protection authorities universally accept that backup media cannot always be cleared instantaneously upon a single user deletion request. However, to remain compliant:

1. **Declared Retention Window:** Organizations must define a strict backup retention policy (e.g., 30, 60, or 90 days).
2. **Immutable Expiry:** Backups, VM snapshots, and database mirrors containing the application's file directories must be completely overwritten or destroyed automatically once they age out of the retention window.
3. **Restoration Protocol:** If a historical backup must be restored to production, administrators must re-run a compliance synchronization script to re-delete files for any users who requested erasure during the intervening period.
