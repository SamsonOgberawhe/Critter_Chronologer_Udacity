package com.udacity.jdnd.course3.critter.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

//@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "Required information was missing from the request.")
@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class MissingInfoException extends RuntimeException {

    public MissingInfoException() {
    }

    public MissingInfoException(String message) {
        super(message);
    }
}
