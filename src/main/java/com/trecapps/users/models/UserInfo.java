package com.trecapps.users.models;

import com.trecapps.auth.common.models.TcBrands;
import com.trecapps.auth.common.models.TcUser;
import lombok.Data;

@Data
public class UserInfo {

    TcUser user;
    TcBrands brand;
}
