package com.trecapps.users.models;

import com.microsoft.graph.models.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;

@Component
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserPost {

    boolean accountEnabled = true;

    String displayName;

    @NotNull
    String mailNickname;

    @NotNull
    String userPrincipalName;

    @NotNull
    PasswordProfile passwordProfile;

    @NotNull
    String mobilePhone;
    @NotNull
    OffsetDateTime birthday;

    @Email
    String mail;

    List<String> otherMails;

}
