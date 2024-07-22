package com.trecapps.users.models;

import lombok.Data;

@Data
public class MfaSubmission {

    String code;
    String type;

}
