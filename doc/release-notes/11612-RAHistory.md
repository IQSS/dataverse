# Role Assignment History Tracking

Dataverse can now track the history of role assignments, allowing administrators to see who assigned or revoked roles, when these actions occurred, and which roles were involved. This feature helps with auditing and understanding permission changes over time.

## Key components of this feature:

- **Feature Flag**: The functionality can be enabled/disabled via the `ROLE_ASSIGNMENT_HISTORY` feature flag 
- **UI Integration**: New history panels on permission management pages showing the complete history of role assignments/revocations
- **CSV Export**: Administrators can download the role assignment history for a given collection or dataset (or files in a dataset) as a CSV file directly from the new panels
- **API Access**: New API endpoints provide access to role assignment history in both JSON and CSV formats:
  - `/api/dataverses/{identifier}/assignments/history`
  - `/api/datasets/{identifier}/assignments/history`
  - `/api/datasets/{identifier}/files/assignments/history`
  
All return JSON by default but will return an internationalized CSV if an `Accept: text/csv` header is adde

For more information, see #11612