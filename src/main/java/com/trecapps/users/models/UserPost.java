package com.trecapps.users.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserPost {

    boolean accountEnabled = true;
    @NotNull
    String displayName;

    @NotNull
    String mailNickname;


    String userPrincipalName;

    @NotNull
    PasswordProfile passwordProfile;

    @NotNull
    String mobilePhone;
    @JsonProperty(required = true)
    OffsetDateTime birthday;

    @Email
    String mail;

    List<String> otherMails;

    public UserPostGraph GetGraphObject()
    {
        return new UserPostGraph(accountEnabled, displayName, mailNickname, userPrincipalName, passwordProfile, mobilePhone, mail, otherMails == null ? new ArrayList<>(): otherMails);
    }
}
