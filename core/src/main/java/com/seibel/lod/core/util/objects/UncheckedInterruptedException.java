package com.seibel.lod.core.util.objects;

import java.util.concurrent.CompletionException;

public class UncheckedInterruptedException extends RuntimeException {
    public UncheckedInterruptedException(String message) {
        super(message);
    }
    public UncheckedInterruptedException(Throwable cause) {
        super(cause);
    }
    public UncheckedInterruptedException(String message, Throwable cause) {
        super(message, cause);
    }
    public UncheckedInterruptedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
    public UncheckedInterruptedException() {
        super();
    }

    public static void throwIfInterrupted() {
        if (Thread.currentThread().isInterrupted()) {
            throw new UncheckedInterruptedException();
        }
    }

    public static UncheckedInterruptedException convert(InterruptedException e) {
        return new UncheckedInterruptedException(e);
    }

    public static void rethrowIfIsInterruption(Throwable t) {
        if (t instanceof InterruptedException) {
            throw convert((InterruptedException) t);
        } else if (t instanceof UncheckedInterruptedException) {
            throw (UncheckedInterruptedException) t;
        } else if (t instanceof CompletionException) {
            rethrowIfIsInterruption(t.getCause());
        }
    }
    public static boolean isThrowableInterruption(Throwable t) {
        return t instanceof InterruptedException || t instanceof UncheckedInterruptedException
                || (t instanceof CompletionException && isThrowableInterruption(t.getCause()));
    }
}
