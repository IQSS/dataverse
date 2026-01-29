-- Adding a case-insensitive index related to #11003
--

CREATE UNIQUE INDEX IF NOT EXISTS INDEX_DVOBJECT_authority_protocol_upper_identifier ON dvobject (authority, protocol, UPPER(identifier));