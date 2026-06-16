package com.yk.back.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resource, Object id) {
        super("%s introuvable : %s".formatted(resource, id), HttpStatus.NOT_FOUND);
    }
}
