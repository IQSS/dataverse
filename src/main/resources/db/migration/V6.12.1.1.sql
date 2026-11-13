-- Add the Scoped Superuser role
INSERT INTO dataverserole (alias, name, description, permissionbits, owner_id)
VALUES ('scopedSuperuser', 'Scoped Superuser', 'A person who can take any action on a dvobject that a superuser could.', 65536, NULL)
ON CONFLICT (alias) DO NOTHING;
