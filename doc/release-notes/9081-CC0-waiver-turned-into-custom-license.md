In an earlier Dataverse release, Datasets with only 'CC0 Waiver' in termsofuse field were converted to 'Custom License' instead of CC0 1.0 licenses during an automated process. A new process was added to correct this. Only Datasets with no terms other than the one create by the previous process will be modified.
- The existing 'Terms of Use' must be equal to 'This dataset is made available under a Creative Commons CC0 license with the following additional/modified terms and conditions: CC0 Waiver'
- The following terms fields must be empty: Confidentiality Declaration, Special Permissions, Restrictions, Citation Requirements, Depositor Requirements, Conditions, and Disclaimer.
- The License ID must not be assigned.

This process will set the License ID to that of the CC0 1.0 license and remove the contents of termsofuse field.
