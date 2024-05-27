package com.trecapps.users.models;

import lombok.Data;

import java.util.Optional;

@Data
public class IdBodyExtender<T> {

    public IdBodyExtender(T b, String i, String u){
        this.id = i;
        this.body = Optional.of(b);
        this.userName = u;
    }

    public IdBodyExtender(T b, String i){
        this(b,i,null);
    }

    public IdBodyExtender(String i, String u){
        this.id = i;
        this.body = Optional.empty();
        this.userName = u;
    }

    public IdBodyExtender(String i){
        this(i,null);
    }



    String userName;
    String id;
    Optional<T> body;

    public boolean isEmpty(){
        return body.isEmpty();
    }

    public T getFullBody(){
        return body.get();
    }
}
