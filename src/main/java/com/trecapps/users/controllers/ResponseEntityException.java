package com.trecapps.users.controllers;

import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

@Data
public class ResponseEntityException extends RuntimeException {
    ResponseEntityException(Object body, HttpStatus status){
        this.body = body;
        this.status = status;
    }

    Object body;
    HttpStatus status;

    ResponseEntity getEntity(){
        return new ResponseEntity<>(body, status);
    }

    Mono<ResponseEntity> getEntityMono(){
        return Mono.just(getEntity());
    }
}
