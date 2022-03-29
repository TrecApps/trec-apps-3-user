package com.trecapps.users.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.nio.CharBuffer;

@Component
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PasswordProfile {

    boolean forceChangePasswordNextSignIn = false;

    boolean forceChangePasswordNextSignInWithMfa = false;

    String password;


}
