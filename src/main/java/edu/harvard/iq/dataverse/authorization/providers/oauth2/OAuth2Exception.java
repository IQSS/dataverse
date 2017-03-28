package edu.harvard.iq.dataverse.authorization.providers.oauth2;

/**
 * Captures errors thrown during the OAuth process.
 * @author michael
 */
public class OAuth2Exception extends Exception {
    
    private final int httpReturnCode;
    private final String messageBody;

    public OAuth2Exception(int httpReturnCode, String messageBody, String message) {
        super(message);
        this.httpReturnCode = httpReturnCode;
        this.messageBody = messageBody;
    }

    public OAuth2Exception(int httpReturnCode, String messageBody, String message, Throwable cause) {
        super(message, cause);
        this.httpReturnCode = httpReturnCode;
        this.messageBody = messageBody;
    }

    public int getHttpReturnCode() {
        return httpReturnCode;
    }

    public String getMessageBody() {
        return messageBody;
    }

}
