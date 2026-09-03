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
    ALTER TABLE template ADD COLUMN IF NOT EXISTS termsofuseorlicense_id BIGINT;
    ALTER TABLE filemetadata ADD COLUMN IF NOT EXISTS termsofuseorlicense_id BIGINT;

    CREATE INDEX IF NOT EXISTS index_datasetversion_default_termsofuseorlicense_id
        ON datasetversion (default_termsofuseorlicense_id);

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
        ALTER TABLE template ADD CONSTRAINT fk_template_termsofuseorlicense_id FOREIGN KEY (termsofuseorlicense_id) REFERENCES termsofuseorlicense(id);
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
        WHERE termsofaccess_id IS NULL OR default_termsofuseorlicense_id IS NULL;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='template' AND column_name='termsofuseandaccess_id') THEN
        UPDATE template
        SET termsofaccess_id = termsofuseandaccess_id,
            termsofuseorlicense_id = termsofuseandaccess_id
        WHERE termsofaccess_id IS NULL OR termsofuseorlicense_id IS NULL;
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

    -- Grant privileges to dvnuser for the new tables and sequences

    GRANT SELECT, INSERT, UPDATE, DELETE ON termsofaccess TO dvnuser;
    GRANT SELECT, INSERT, UPDATE, DELETE ON termsofuseorlicense TO dvnuser;
    GRANT USAGE, SELECT ON SEQUENCE termsofaccess_id_seq TO dvnuser;
    GRANT USAGE, SELECT ON SEQUENCE termsofuseorlicense_id_seq TO dvnuser;
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
