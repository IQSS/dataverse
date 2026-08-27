### URL signing fixed and hardened

Signed URLs now work for URLs containing special characters (broken in 6.10, e.g. for `doi:` persistent identifiers), and the signing secret is now generated automatically and stored in the database: the `dataverse.api.signing-secret` JVM option is no longer used, and any value set there is ignored after upgrading. See [:ApiSigningSecret](https://guides.dataverse.org/en/latest/installation/config.html#apisigningsecret) in the Configuration Guide for details, including rotation.
