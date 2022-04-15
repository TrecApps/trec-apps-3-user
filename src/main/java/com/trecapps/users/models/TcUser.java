package com.trecapps.users.models;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Component
public class TcUser {

    public TcUser(UserPost post, String id)
    {
        this.id = id;

        displayName = post.getDisplayName();
        post.getBirthday();

        birthday = post.getBirthday();
        birthdaySetting = "private"; // Assume User does not want their birthday seen by anyone

        userProfile = post.getUserPrincipalName();

        mobilePhone = post.getMobilePhone();
        phoneVerified = false;

        email = post.mail;
        emailVerified = false;

        credibilityRating = 5;
    }

    // Core Info
    String id;
    String profilePic;
    String displayName;
    String userProfile;

    // Phone Used by the User
    String mobilePhone;
    boolean phoneVerified;

    // External Email used by the User
    String email;
    boolean emailVerified;

    // Aides in phone/Email Verification
    String currentCode;
    OffsetDateTime codeExpiration;

    // Birthday
    OffsetDateTime birthday;
    String birthdaySetting;

    // Addresses used by the User
    String[] address;

    // External Profiles
    List<UUID> brands;
    Map<UUID, UUID> brandSettings; // Device/App setting determining which Brand the User is currently id-ing as

    String restrictions; // Semicolon restricted details on the claims against this user

    // Credibility rating (used by services like Falsehoods to assess how credible this user is)
    long credibilityRating;
}
