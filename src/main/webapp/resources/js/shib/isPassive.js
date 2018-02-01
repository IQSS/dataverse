/*
 This isPassive script will automatically try to log in a user using the SAML2
 isPassive feature.
 In case a user already has an authenticated session at his Identity Provider and
 given the Discovery Service can guess the user's Identity Provider, the user will
 eventually be on the exact same page this script is embedded in but logged in
 (= Shibboleth attributes are available and user has a valid session with the
 Service Provider on the same host).
 The user page also will be requested with the same GET arguments than the initial request.

 Requirements:
 - Only works if a Service Provider 2.x is installed on the same host
 - JavaScript must be enabled. Otherwise the script won't have any effect.
 - The script must be able to set cookies (required for Shibboleth Service Provider as well)
 - In the shibboleth2.xml there must be defined a redirectErrors="#THIS PAGE#" in
 the <Errors> element. #THIS PAGE# must be the relative/absolute URL of the page
 this script is embedded in.
 - It also makes sense to protect #THIS PAGE# with a lazy session in order to use
 the Shibboleth attribute that should be available after authentication.
 */


// QDR Custom - for Jira QDR-891, remove a cookie to trigger shib passive login for any qdr drupal windows, 
// and set cookie to stop passive login for dv
var loginLink = document.getElementById("login");
if(loginLink != null) {
  loginLink.onclick = function() {
    document.cookie = "_check_is_passive=;expires=Thu, 01 Jan 1970 00:00:01 GMT;domain=" + window.location.hostname.substring(window.location.hostname.indexOf("."));
    document.cookie = "_check_is_passive_dv=0;domain=" + window.location.hostname.substring(window.location.hostname.indexOf("."));
  };
}


// Check for session cookie that contains the initial location
if(document.cookie && document.cookie.search(/_check_is_passive_dv=/) >= 0){
    // If we have the opensaml::FatalProfileException GET arguments
    // redirect to initial location because isPassive failed
    if (
        window.location.search.search(/errorType/) >= 0
        && window.location.search.search(/RelayState/) >= 0
        && window.location.search.search(/requestURL/) >= 0
    ) {
        var cookieStr = document.cookie + ";";
        var startpos = (cookieStr.indexOf('_check_is_passive_dv=')+21);
        var endpos = cookieStr.indexOf(';', startpos);
        window.location = cookieStr.substring(startpos,endpos);
    }
} else {
    // Mark browser as being isPassive checked
	//QDR Custom - add a domain shared by drupal and dv components
	//QDR-901 - set cookie path to / (should really be drupal root) to make sure passive login check only happens once, even when initial page is at a path (e.g. /discover/pilots)
    document.cookie = "_check_is_passive_dv=" + window.location + ";path=/;domain=" + window.location.hostname.substring(window.location.hostname.indexOf("."));;

    // Redirect to Shibboleth handler
    //QDR-901 - include query in redirect to avoid changing result after passive check
    window.location = "/Shibboleth.sso/Login?isPassive=true&target=" + encodeURIComponent("https://" + window.location.hostname + "/shib.xhtml?redirectPage=" + window.location.pathname + window.location.search);
}
