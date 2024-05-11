package org.insta.content.exception.post;

/**
 * <p>
 * Exception class for handle post update.
 * </p>
 *
 * @author Mohamed Yasar
 * @version 1.0 6 Feb 2024
 * @see PostException
 */
public final class PostUpdateFailedException extends PostException {

    /**
     * <p>
     * Constructs an PostUpdateFailedException with the specified detail message.
     * </p>
     *
     * @param message Refers the exception message.
     */
    public PostUpdateFailedException(final String message) {
        super(message);
    }
}
