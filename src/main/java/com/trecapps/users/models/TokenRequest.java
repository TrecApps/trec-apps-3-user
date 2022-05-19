package com.trecapps.users.models;

import lombok.Data;



@Data
public class TokenRequest {
    String code;
    String state;
    String redirect;
}
