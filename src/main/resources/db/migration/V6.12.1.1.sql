CREATE TABLE IF NOT EXISTS termsofaccess (
                       id  BIGSERIAL NOT NULL,
                       availabilitystatus TEXT,
                       contactforaccess TEXT,
                       dataaccessplace TEXT,
                       originalarchive TEXT,
                       sizeofcollection TEXT,
                       studycompletion TEXT,
                       termsofaccess TEXT,
                       fileaccessrequest BOOLEAN,
                       PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS termsofuseorlicense (
                             id  BIGSERIAL NOT NULL,
                             citationrequirements TEXT,
                             conditions TEXT,
                             confidentialitydeclaration TEXT,
                             depositorrequirements TEXT,
                             disclaimer TEXT,
                             license_id BIGINT,
                             restrictions TEXT,
                             specialpermissions TEXT,
                             termsofuse TEXT,
                             PRIMARY KEY (id)
);

DO $$
BEGIN
    ALTER TABLE datasetversion ADD COLUMN IF NOT EXISTS termsofaccess_id BIGINT;
    ALTER TABLE datasetversion ADD COLUMN IF NOT EXISTS default_termsofuseorlicense_id BIGINT;
    ALTER TABLE template ADD COLUMN IF NOT EXISTS termsofaccess_id BIGINT;
    ALTER TABLE template ADD COLUMN IF NOT EXISTS default_termsofuseorlicense_id BIGINT;
    ALTER TABLE filemetadata ADD COLUMN IF NOT EXISTS termsofuseorlicense_id BIGINT;

    CREATE INDEX IF NOT EXISTS index_datasetversion_default_termsofuseorlicense_id
        ON datasetversion (default_termsofuseorlicense_id);

    CREATE INDEX IF NOT EXISTS index_datasetversion_termsofaccess_id
        ON datasetversion (termsofaccess_id);

    BEGIN
        ALTER TABLE termsofuseorlicense ADD CONSTRAINT fk_termsofuseorlicense_license_id FOREIGN KEY (license_id) REFERENCES license(id);
    EXCEPTION
        WHEN duplicate_object THEN RAISE NOTICE 'Table constraint fk_termsofuseorlicense_license_id already exists';
    END;
    BEGIN
        ALTER TABLE datasetversion ADD CONSTRAINT fk_datasetversion_default_termsofuseorlicense_id FOREIGN KEY (default_termsofuseorlicense_id) REFERENCES termsofuseorlicense(id);
    EXCEPTION
        WHEN duplicate_object THEN RAISE NOTICE 'Table constraint fk_datasetversion_default_termsofuseorlicense_id already exists';
    END;
    BEGIN
        ALTER TABLE datasetversion ADD CONSTRAINT fk_datasetversion_termsofaccess_id FOREIGN KEY (termsofaccess_id) REFERENCES termsofaccess(id);
    EXCEPTION
        WHEN duplicate_object THEN RAISE NOTICE 'Table constraint fk_datasetversion_termsofaccess_id already exists';
    END;
    BEGIN
        ALTER TABLE template ADD CONSTRAINT fk_template_default_termsofuseorlicense_id FOREIGN KEY (default_termsofuseorlicense_id) REFERENCES termsofuseorlicense(id);
    EXCEPTION
        WHEN duplicate_object THEN RAISE NOTICE 'Table constraint fk_template_termsofuseorlicense_id already exists';
    END;
    BEGIN
        ALTER TABLE template ADD CONSTRAINT fk_template_termsofaccess_id FOREIGN KEY (termsofaccess_id) REFERENCES termsofaccess(id);
    EXCEPTION
        WHEN duplicate_object THEN RAISE NOTICE 'Table constraint fk_template_termsofaccess_id already exists';
    END;
    BEGIN
        ALTER TABLE filemetadata ADD CONSTRAINT fk_filemetadata_termsofuseorlicense_id FOREIGN KEY (termsofuseorlicense_id) REFERENCES termsofuseorlicense(id);
    EXCEPTION
        WHEN duplicate_object THEN RAISE NOTICE 'Table constraint fk_filemetadata_termsofuseorlicense_id already exists';
    END;

    -- Migrate data from the old table termsofuseandaccess to the new tables termsofuseorlicense and termsofaccess

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='public' AND table_name='termsofuseandaccess') THEN
        INSERT INTO termsofuseorlicense (id, citationrequirements, conditions, confidentialitydeclaration, depositorrequirements, disclaimer, license_id, restrictions, specialpermissions, termsofuse)
        SELECT id, citationrequirements, conditions, confidentialitydeclaration, depositorrequirements, disclaimer, license_id, restrictions, specialpermissions, termsofuse
        FROM termsofuseandaccess
        ON CONFLICT (id) DO NOTHING;

        INSERT INTO termsofaccess (id, availabilitystatus, contactforaccess, dataaccessplace, originalarchive, sizeofcollection, studycompletion, termsofaccess, fileaccessrequest)
        SELECT id, availabilitystatus, contactforaccess, dataaccessplace, originalarchive, sizeofcollection, studycompletion, termsofaccess, fileaccessrequest
        FROM termsofuseandaccess
        ON CONFLICT (id) DO NOTHING;

    END IF;

    -- Migrate old references to termsofuseandaccess to termsofaccess and termsofuseorlicense in datasetversion and template tables

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='datasetversion' AND column_name='termsofuseandaccess_id') THEN
        UPDATE datasetversion
        SET termsofaccess_id = termsofuseandaccess_id,
            default_termsofuseorlicense_id = termsofuseandaccess_id
        WHERE termsofaccess_id IS NULL AND default_termsofuseorlicense_id IS NULL;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='template' AND column_name='termsofuseandaccess_id') THEN
        UPDATE template
        SET termsofaccess_id = termsofuseandaccess_id,
            default_termsofuseorlicense_id = termsofuseandaccess_id
        WHERE termsofaccess_id IS NULL AND default_termsofuseorlicense_id IS NULL;
    END IF;

    -- Drop the old termsofuseandaccess references, since we have migrated the data to the new tables.

    DROP INDEX IF EXISTS index_datasetversion_termsofuseandaccess_id;
    ALTER TABLE public.datasetversion DROP CONSTRAINT IF EXISTS fk_datasetversion_termsofuseandaccess_id;
    ALTER TABLE datasetversion DROP COLUMN IF EXISTS termsofuseandaccess_id;

    ALTER TABLE public.template DROP CONSTRAINT IF EXISTS fk_template_termsofuseandaccess_id;
    ALTER TABLE template DROP COLUMN IF EXISTS termsofuseandaccess_id;

    -- termsofuseorlicense for filemetadata prepares the new functionality, we don't need to drop old references from this table.

    -- Drop the old termsofuseandaccess table, since we migrated the content and dropped the references

    ALTER TABLE IF EXISTS public.termsofuseandaccess DROP CONSTRAINT IF EXISTS fk_termsofuseandaccess_license_id;
    ALTER TABLE IF EXISTS public.termsofuseandaccess DROP CONSTRAINT IF EXISTS fk_termsofuseandcesss_license_id; -- typo in the past? cleaning up anyway
    DROP TABLE IF EXISTS termsofuseandaccess;

END
$$;

DO $$
BEGIN
    -- Reset the sequences for termsofaccess and termsofuseorlicense tables to the max id value.
    -- We have migrated data from the old termsofuseandaccess table, and we want to avoid conflicts with new inserts.
    IF EXISTS (SELECT 1 FROM termsofaccess) THEN
        PERFORM setval('termsofaccess_id_seq', MAX(id)) FROM termsofaccess;
    END IF;
    IF EXISTS (SELECT 1 FROM termsofuseorlicense) THEN
        PERFORM setval('termsofuseorlicense_id_seq', MAX(id)) FROM termsofuseorlicense;
    END IF;
END
$$;

-- ============================================================================
-- Seed data from migrations prior to V5.10.1.3.
-- These are skipped on fresh installations where EclipseLink creates tables
-- from the current entity model and Flyway baselines at V5.10.1.3.
-- ============================================================================

-- Default CC0 license (V5.9.0.1).
-- The application calls LicenseServiceBean.getDefault() in multiple places
-- (DatasetPage, TemplatePage, JSONLDUtil, SwordServiceBean, etc.) and will
-- crash with NoResultException if no default license exists.
DO $$
BEGIN
  BEGIN
    INSERT INTO license (uri, name, shortdescription, active, isDefault, iconurl, sortorder)
    VALUES ('http://creativecommons.org/publicdomain/zero/1.0', 'CC0 1.0',
            'Creative Commons CC0 1.0 Universal Public Domain Dedication.',
            true, true, 'https://licensebuttons.net/p/zero/1.0/88x31.png', 1);
  EXCEPTION
    WHEN unique_violation THEN RAISE NOTICE 'CC0 license has already been added to the license table';
  END;
END $$;

-- ============================================================================
-- PostgreSQL-specific indexes and constraints from migrations prior to V6.3.0.1.
-- These are skipped on fresh installations where EclipseLink creates tables
-- from the current entity model and Flyway baselines at V5.10.1.3.
-- Only PostgreSQL-specific features that cannot be defined in JPA are included here.
-- Items already defined in JPA @Table annotations are handled by EclipseLink and
-- are not duplicated here.
-- ============================================================================

-- Partial unique index preventing multiple draft versions per dataset (V5.2.0.1).
-- Cannot be defined in JPA — PostgreSQL-specific partial index.
CREATE UNIQUE INDEX IF NOT EXISTS one_draft_version_per_dataset ON datasetversion (dataset_id) WHERE versionstate='DRAFT';

-- Partial unique index preventing duplicate file access requests (V6.0.0.1).
-- Cannot be defined in JPA — PostgreSQL-specific partial index.
CREATE UNIQUE INDEX IF NOT EXISTS created_requests ON fileaccessrequests (datafile_id, authenticated_user_id) WHERE request_state='CREATED';

-- Fix typo in externalvocabularyvalue URI unique constraint name (V6.1.0.3).
-- On existing databases, flyway may have auto-generated "externalvocabularvalue_uri_key" (missing "y").
-- Drop the misspelled version and create the correctly spelled one.
-- On fresh databases, EclipseLink creates "externalvocabularyvalue_uri_key" directly, so the DROP is a no-op
-- and the ADD is caught by the duplicate_object exception handler.
ALTER TABLE externalvocabularyvalue DROP CONSTRAINT IF EXISTS externalvocabularvalue_uri_key;
DO $$
BEGIN
  BEGIN
    ALTER TABLE externalvocabularyvalue ADD CONSTRAINT externalvocabularyvalue_uri_key UNIQUE(uri);
  EXCEPTION
    WHEN duplicate_object THEN RAISE NOTICE 'Table constraint externalvocabularyvalue_uri_key already exists';
  END;
END $$;

-- CHECK constraint on dvobject storageidentifier (V5.11.1.6).
-- Cannot be defined in JPA — PostgreSQL-specific CHECK constraint.
DO $$
BEGIN
  BEGIN
    ALTER TABLE dvobject ADD CONSTRAINT chk_dvobject_storageidentifier CHECK (strpos(storageidentifier,'..') = 0);
  EXCEPTION
    WHEN duplicate_object THEN RAISE NOTICE 'Table constraint chk_dvobject_storageidentifier already exists';
  END;
END $$;

-- Named foreign key on dataset.template_id (V5.11.1.4).
-- EclipseLink creates an unnamed FK via @ManyToOne; this ensures a named constraint
-- for consistency with legacy databases.
DO $$
BEGIN
  BEGIN
    ALTER TABLE dataset ADD CONSTRAINT fx_dataset_template_id FOREIGN KEY (template_id) REFERENCES template(id);
  EXCEPTION
    WHEN duplicate_object THEN RAISE NOTICE 'Table constraint fx_dataset_template_id already exists';
  END;
END $$;

-- estimateGuestBookResponseTableSize function (V6.0.0.3).
-- Replaces the version from V5.8.0.3.
CREATE OR REPLACE FUNCTION estimateGuestBookResponseTableSize()
RETURNS bigint AS $$
DECLARE
  estimatedsize bigint;
BEGIN
  SELECT CASE WHEN relpages<10 THEN 0
              ELSE ((reltuples / relpages)
               * (pg_relation_size('public.guestbookresponse') / current_setting('block_size')::int))::bigint
               * (SELECT CASE WHEN ((select count(*) from pg_stats where tablename='guestbookresponse') = 0 
                   OR (select array_position(most_common_vals::text::text[], 'AccessRequest') 
                       FROM pg_stats WHERE tablename='guestbookresponse' AND attname='eventtype') IS NULL) THEN 1
                   ELSE 1 - (SELECT (most_common_freqs::text::text[])[array_position(most_common_vals::text::text[], 'AccessRequest')]::float
                       FROM pg_stats WHERE tablename='guestbookresponse' and attname='eventtype') END)
         END
     FROM   pg_class
     WHERE  oid = 'public.guestbookresponse'::regclass INTO estimatedsize;

     if estimatedsize = 0 then
     SELECT COUNT(id) FROM guestbookresponse WHERE eventtype!= 'AccessRequest' INTO estimatedsize;
     END if;   

  RETURN estimatedsize;
END;
$$ LANGUAGE plpgsql IMMUTABLE;
