package com.ecommerce.exception;

import org.springframework.http.HttpStatus;

public class ResourceAlreadyExistsException extends ApiException {

    public ResourceAlreadyExistsException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
