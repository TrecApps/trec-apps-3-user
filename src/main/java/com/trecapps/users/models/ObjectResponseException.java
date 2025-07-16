package com.trecapps.users.models;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

public class ObjectResponseException extends RuntimeException {
    HttpStatus status;
    public ObjectResponseException(HttpStatus status, String message){
        super(message);
        this.status = status;
    }

    public ResponseObj toResponseObject(){
        return ResponseObj.getInstance(status, getMessage());
    }

    public ResponseEntity<ResponseObj> toResponseEntity(){
        return new ResponseEntity<>(toResponseObject(), HttpStatusCode.valueOf(status.value()));
    }


}
