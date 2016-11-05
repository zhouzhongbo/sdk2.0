package com.droi.exceptions;

public class UrlParseException extends Exception {
    public UrlParseException(final String detailMessage) {
        super(detailMessage);
    }

    public UrlParseException(final Throwable throwable) {
        super(throwable);
    }
}
