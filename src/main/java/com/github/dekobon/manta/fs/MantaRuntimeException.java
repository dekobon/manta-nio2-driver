package com.github.dekobon.manta.fs;

/**
 * Runtime exception used to wrap compile-time exceptions from the Manta Java
 * library so that we can play nice with the NIO.2 API.
 *
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
