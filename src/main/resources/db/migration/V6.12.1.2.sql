-- Banner messages can be hidden behind an icon (collapsed) until clicked.
ALTER TABLE bannermessage ADD COLUMN IF NOT EXISTS collapsed BOOLEAN NOT NULL DEFAULT FALSE;
