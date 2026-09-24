## BUG ##
/api/datasets/{id}/download/count?includeMDC= was including the MDCStartDate when the parameter was false and null when true. Also, the 'downloadCount' was always being retrieved from the guestbook response count. This fix returns the downloadCount, viewCount, and number of citations from the MDC metrics when the parameter is passed in as 'true'. 
