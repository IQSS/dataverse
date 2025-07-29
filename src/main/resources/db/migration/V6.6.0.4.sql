/* We're adding new permissions at bit 13 (4096 LinkDataverse) and bit 14 (8192 LinkDataset).
They should be set for any role that has the permission in bit 11 (1024 PublishDataverse) and bit 12 (2048 PublishDataset),
respectively.
We also need to move the permissions with current bits 13-14 to bits 15-16 to make room for the new permissions.

Finally, to make this idempotent, at least under the assumption that the standard admin role with all permissions exists
(or some role with current permission bit 13 (DeleteDataverse) or 14 (DeleteDatasetDraft) set), we check to make sure no
role already has bit 15-16 set before applying the migration.

*/
DO
$do$
    BEGIN
        -- Skip if migration already applied (i.e., bits 15-16 in use)
        IF (SELECT MAX(permissionbits) FROM dataverserole) < 16384 THEN

            -- If bit 13 is set, move it to bit 15 and clear it
            UPDATE dataverserole
            SET permissionbits = (permissionbits | 16384) & ~4096
            WHERE (permissionbits & 4096) != 0;

            -- If bit 14 is set, move it to bit 16 and clear it
            UPDATE dataverserole
            SET permissionbits = (permissionbits | 32768) & ~8192
            WHERE (permissionbits & 8192) != 0;

            -- Set bit 13 (4096 LinkDataverse) if bit 11 (1024 PublishDataverse) is set
            UPDATE dataverserole
            SET permissionbits = permissionbits | 4096
            WHERE (permissionbits & 1024) != 0;

            -- Set bit 14 (8192 LinkDataset) if bit 12 (2048 PublishDataset) is set
            UPDATE dataverserole
            SET permissionbits = permissionbits | 8192
            WHERE (permissionbits & 2048) != 0;

        END IF;
    END
$do$;