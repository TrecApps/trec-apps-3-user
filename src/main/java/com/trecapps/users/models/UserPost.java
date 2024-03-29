package com.trecapps.users.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trecapps.auth.models.PhoneNumber;
import com.trecapps.auth.models.TcUser;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
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
    Long mobilePhone;
    @JsonProperty(required = true)
    OffsetDateTime birthday;

    @Email
    String mail;

    List<String> otherMails;

    public UserPostGraph GetGraphObject()
    {
        return new UserPostGraph(accountEnabled, displayName, mailNickname, userPrincipalName, passwordProfile, mobilePhone.toString(), mail, otherMails == null ? new ArrayList<>(): otherMails);
    }

    public TcUser GetTcUserObject(){
        TcUser ret = new TcUser();
        ret.setDisplayName(displayName);
        ret.setBirthday(birthday);

        ret.setBirthdaySetting("private"); // Assume User does not want their birthday seen by anyone

        ret.setUserProfile(userPrincipalName);

        var num = new PhoneNumber();
        num.setNumber(mobilePhone);
        ret.setMobilePhone(num);
        ret.setPhoneVerified(false);

        ret.setEmail(mail);
        ret.setEmailVerified(false);

        ret.setCredibilityRating(5);


        return ret;
    }
}
