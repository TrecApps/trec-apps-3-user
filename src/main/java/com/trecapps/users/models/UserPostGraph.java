package com.trecapps.users.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserPostGraph {
    boolean accountEnabled = true;

    String displayName;

    String mailNickname;

    String userPrincipalName;

    PasswordProfile passwordProfile;

    String mobilePhone;

    String mail;

    List<String> otherMails;
}
