//package com.trecapps.users.models;
//
//import com.trecapps.auth.common.models.PhoneNumber;
//import com.trecapps.auth.common.models.TcUser;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import jakarta.validation.constraints.Email;
//import java.time.OffsetDateTime;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.UUID;
//
//@Data
//@NoArgsConstructor
//public class TcUserRet {
//
//    public TcUserRet(UserPost post, String id)
//    {
//        this.id = id;
//
//        displayName = post.getDisplayName();
//        OffsetDateTime b = post.getBirthday();
//        if(b != null)
//            birthday = b.toString();
//        birthdaySetting = "private"; // Assume User does not want their birthday seen by anyone
//
//        userProfile = post.getUserPrincipalName();
//
//        mobilePhone = post.getMobilePhone();
//        phoneVerified = false;
//
//        email = post.mail;
//        emailVerified = false;
//
//        credibilityRating = 5;
//    }
//
//    public TcUser getAuthUser()
//    {
//        TcUser ret = new TcUser();
//        ret.setAddress(address);
//        ret.setBirthday(OffsetDateTime.parse(birthday));
//        ret.setBirthdaySetting(birthdaySetting);
//        ret.setBrands(brands);
//        ret.setBrandSettings(brandSettings);
//        ret.setCodeExpiration(codeExpiration);
//        ret.setCredibilityRating(credibilityRating);
//        ret.setCurrentCode(currentCode);
//        ret.setDisplayName(displayName);
//        ret.setEmail(email);
//        ret.setEmailVerified(emailVerified);
//        ret.setId(id);
//        if(mobilePhone != null) {
//            var num = new PhoneNumber(mobilePhone);
//            ret.setMobilePhone(num);
//        }
//        ret.setPhoneVerified(phoneVerified);
//        ret.setProfilePic(profilePic);
//        ret.setRestrictions(restrictions);
//        ret.setUserProfile(userProfile);
//
//        return ret;
//    }
//
//    public static TcUser getUserFromAuthUser(TcUser user)
//    {
//        TcUser  ret = new TcUser();
//        ret.setAddress(user.getAddress());
//        ret.setBirthday(user.getBirthday());
//        ret.setBirthdaySetting(user.getBirthdaySetting());
//        ret.setBrands(user.getBrands());
//        ret.setBrandSettings(user.getBrandSettings());
//        ret.setCodeExpiration(user.getCodeExpiration());
//        ret.setCredibilityRating(user.getCredibilityRating());
//        ret.setCurrentCode(user.getCurrentCode());
//        ret.setDisplayName(user.getDisplayName());
//        ret.setEmail(user.getEmail());
//        ret.setEmailVerified(user.isEmailVerified());
//        ret.setId(user.getId());
//        var phone = user.getMobilePhone();
//        if(phone != null)
//            ret.setMobilePhone(phone);
//        ret.setPhoneVerified(user.isPhoneVerified());
//        ret.setProfilePic(user.getProfilePic());
//        ret.setRestrictions(user.getRestrictions());
//        ret.setUserProfile(user.getUserProfile());
//
//        return ret;
//    }
//
//    // Core Info
//    String id;
//    String profilePic;
//    String displayName;
//    String userProfile;
//
//    // Phone Used by the User
//    Long mobilePhone;
//    boolean phoneVerified;
//
//    // External Email used by the User
//    @Email
//    String email;
//    boolean emailVerified;
//
//    // Aides in phone/Email Verification
//    String currentCode;
//    OffsetDateTime codeExpiration;
//
//    // Birthday
//    String birthday;
//    String birthdaySetting;
//
//    // Addresses used by the User
//    List<String> address;
//
//    // External Profiles
//    Set<String> brands;
//    Map<UUID, UUID> brandSettings; // Device/App setting determining which Brand the User is currently id-ing as
//
//    String restrictions; // Semicolon restricted details on the claims against this user
//
//    // Credibility rating (used by services like Falsehoods to assess how credible this user is)
//    long credibilityRating;
//}
