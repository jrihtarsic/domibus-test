package eu.domibus.core.crypto.spi.model;

/**
 * @author Thomas Dussart
 * @since 4.1
 *
 * Exception thrown by the new Authorization module.
 */
public class AuthorizationException extends RuntimeException {

    private String httpCode;

    private AuthorizationError authorizationError;

    private String messageId;

    public AuthorizationException(Throwable e) {
        super(e);
    }

    public AuthorizationException(final AuthorizationError authorizationError, final String message) {
        super(message);
        this.authorizationError = authorizationError;
    }

    public AuthorizationException(final AuthorizationError authorizationError, final Throwable cause) {
        super(cause);
        this.authorizationError = authorizationError;
    }

    public AuthorizationException(final AuthorizationError authorizationError, final String message, final Throwable cause) {
        super(message, cause);
        this.authorizationError = authorizationError;
    }

    protected AuthorizationException(final String httpCode, final String message, final Throwable cause) {
        super(message, cause);
        this.httpCode = httpCode;
    }

    public AuthorizationException(final AuthorizationError authorizationError, final String httpCode, final String messageID, final String message, Throwable throwable) {
        super(message, throwable);
        this.httpCode = httpCode;
        this.authorizationError = authorizationError;
        this.messageId = messageID;
    }

    public String getHttpCode() {
        return httpCode;
    }

    public AuthorizationError getAuthorizationError() {
        return authorizationError;
    }

    public String getMessageId() {
        return messageId;
    }
}
