package edu.harvard.iq.dataverse.authorization;

/**
 * A result of an authentication attempt. May succeed, fail, or be in error.
 * Client code may use normal constructors, or use one of the static convenience 
 * methods ({@code createXXX}).
 * 
 * @author michael
 */
public class AuthenticationResponse {
    
    public static AuthenticationResponse makeSuccess( String userId, AuthenticatedUserDisplayInfo disInf ) {
        return new AuthenticationResponse()
               .setStatus( Status.SUCCESS )
               .setUserId(userId)
               .setUserDisplayInfo(disInf);
    }
    
    public static AuthenticationResponse makeBreakout( String userId, String redirectUrl ) {
        return new AuthenticationResponse()
               .setStatus( Status.BREAKOUT )
               .setUserId(userId)
               .setMessage(redirectUrl);
    }
    
    public static AuthenticationResponse makeFail( String message ) {
        return new AuthenticationResponse()
               .setStatus( Status.FAIL )
               .setMessage(message);
    }
    
    public static AuthenticationResponse makeError( String message, Throwable t ) {
        return new AuthenticationResponse()
               .setStatus( Status.ERROR )
               .setMessage(message)
               .setError(t);
    }
    
    public enum Status { 
        /** Authentication succeeded - go on to the next phase */
        SUCCESS,
        
        /** UserProvider wants to take the user through some process. Go to link in the message field */
        BREAKOUT,
        
        /** Authentication failed (e.g wrong password) */
        FAIL,
        
        /** Can't authenticate (e.g database is down) */
        ERROR
    }
    
    private Status status;
    private String message;
    private Throwable error;
    private String userId;
    private AuthenticatedUserDisplayInfo userDisplayInfo;

    public Status getStatus() {
        return status;
    }

    public AuthenticationResponse setStatus(Status status) {
        this.status = status;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public AuthenticationResponse setMessage(String message) {
        this.message = message;
        return this;
    }

    public Throwable getError() {
        return error;
    }

    public AuthenticationResponse setError(Throwable error) {
        this.error = error;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public AuthenticationResponse setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public AuthenticatedUserDisplayInfo getUserDisplayInfo() {
        return userDisplayInfo;
    }

    public AuthenticationResponse setUserDisplayInfo(AuthenticatedUserDisplayInfo userDisplayInfo) {
        this.userDisplayInfo = userDisplayInfo;
        return this;
    }
    
    
    
}
