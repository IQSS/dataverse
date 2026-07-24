## Bug ##
API /api/guestbooks/{id}/responses was returning incorrect stats in the pagination block. This corrects the counts by including the full hierarchy of parent collections.
