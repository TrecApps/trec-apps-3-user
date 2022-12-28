package com.trecapps.users.models;

import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
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

    public com.trecapps.auth.models.TcUser getAuthUser()
    {
        com.trecapps.auth.models.TcUser ret = new com.trecapps.auth.models.TcUser();
        ret.setAddress(address);
        ret.setBirthday(birthday);
        ret.setBirthdaySetting(birthdaySetting);
        ret.setBrands(brands);
        ret.setBrandSettings(brandSettings);
        ret.setCodeExpiration(codeExpiration);
        ret.setCredibilityRating(credibilityRating);
        ret.setCurrentCode(currentCode);
        ret.setDisplayName(displayName);
        ret.setEmail(email);
        ret.setEmailVerified(emailVerified);
        ret.setId(id);
        ret.setMobilePhone(mobilePhone);
        ret.setPhoneVerified(phoneVerified);
        ret.setProfilePic(profilePic);
        ret.setRestrictions(restrictions);
        ret.setUserProfile(userProfile);

        return ret;
    }

    public static TcUser getUserFromAuthUser(com.trecapps.auth.models.TcUser user)
    {
        TcUser  ret = new TcUser();
        ret.setAddress(user.getAddress());
        ret.setBirthday(user.getBirthday());
        ret.setBirthdaySetting(user.getBirthdaySetting());
        ret.setBrands(user.getBrands());
        ret.setBrandSettings(user.getBrandSettings());
        ret.setCodeExpiration(user.getCodeExpiration());
        ret.setCredibilityRating(user.getCredibilityRating());
        ret.setCurrentCode(user.getCurrentCode());
        ret.setDisplayName(user.getDisplayName());
        ret.setEmail(user.getEmail());
        ret.setEmailVerified(user.isEmailVerified());
        ret.setId(user.getId());
        ret.setMobilePhone(user.getMobilePhone());
        ret.setPhoneVerified(user.isPhoneVerified());
        ret.setProfilePic(user.getProfilePic());
        ret.setRestrictions(user.getRestrictions());
        ret.setUserProfile(user.getUserProfile());

        return ret;
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
    @Email
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
    Set<UUID> brands;
    Map<UUID, UUID> brandSettings; // Device/App setting determining which Brand the User is currently id-ing as

    String restrictions; // Semicolon restricted details on the claims against this user

    // Credibility rating (used by services like Falsehoods to assess how credible this user is)
    long credibilityRating;
}
