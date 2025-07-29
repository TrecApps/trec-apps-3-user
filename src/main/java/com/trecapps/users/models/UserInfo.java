package com.trecapps.users.models;

import com.trecapps.auth.common.models.TcBrands;
import com.trecapps.auth.common.models.TcUser;
import lombok.Data;

import java.util.List;

@Data
public class UserInfo {

    TcUser user;
    TcBrands brand;

    List<TcBrands> ownedAccounts;
}
