## Contact Email Improvements

Email sent from the contact forms to the contact(s) for a collection, dataset, or datafile can now optionally be cc'd to a support email address. The support email address can be changed from the default :SystemEmail address to a separate :SupportEmail address. When multiple contacts are listed, the system will now send one email to all contacts (with the optional cc if configured) instead of separate emails to each contact. Contact names with a comma that refer to Organizations will no longer have the name parts reversed in the email greeting. A new protected feedback API has been added.

## Backward Incompatibilities

When there are multiple contacts, the system will now send one email with all of the contacts in the To: header instead of sending one email to each contact (with no indication that others have been notified).

## New JVM/MicroProfile Settings

dataverse.mail.support-email - allows a separate email, distinct from the :SystemEmail to be used as the reply-to address in emails from the contact form/ feedback api.
dataverse.mail.cc-support-on-contact-emails - include the support email address as a CC: entry when contact/feedback emails are sent to the contacts for a collection, dataset, or datafile.