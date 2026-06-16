package com.yk.back.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends BusinessException {

    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }

    public ForbiddenException() {
        super("Accès refusé", HttpStatus.FORBIDDEN);
    }
}
