package com.github.dekobon.manta.fs;

/**
 * @author Elijah Zupancic
 * @since 1.0.0
 */
public class MantaRuntimeException extends RuntimeException {
    public MantaRuntimeException() {
    }

    public MantaRuntimeException(String message) {
        super(message);
    }

    public MantaRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public MantaRuntimeException(Throwable cause) {
        super(cause);
    }

    public MantaRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
