package com.trecapps.users.models;

import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Data
public class ResponseObj {

    transient HttpStatus httpStatus;
    int status;

    String message;
    String id;

    public static ResponseObj getInstance(HttpStatus httpStatus1, String message){
        ResponseObj ret = new ResponseObj();
        ret.httpStatus = httpStatus1;
        ret.status = httpStatus1.value();
        ret.message = message;
        return ret;
    }

    public static ResponseObj getInstance(String message){
        return getInstance(HttpStatus.OK, message);
    }

    public static ResponseObj getInstance(String message, String id){
        ResponseObj ret = getInstance(message);
        ret.id = id;
        return ret;
    }

    public ResponseEntity<ResponseObj> toEntity(){
        return new ResponseEntity<>(this, httpStatus);
    }
}
