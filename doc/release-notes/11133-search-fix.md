### Search fix when using AVOID_EXPENSIVE_SOLR_JOIN=true

Dataverse v6.5 introduced a bug which causes search to fail for non-superusers in multiple groups when the AVOID_EXPENSIVE_SOLR_JOIN feature flag is set to true. This releases fixes the bug.
