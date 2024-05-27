package com.trecapps.users.models;


import com.trecapps.auth.common.models.TrecAuthentication;
import lombok.Getter;

@Getter
public class AuthenticationBody<T> {
    TrecAuthentication authentication;
    T data;

    public AuthenticationBody(TrecAuthentication authentication1, T data1){
        this.authentication = authentication1;
        this.data = data1;
    }
}
