package com.trecapps.users.controllers;

import com.azure.core.annotation.Get;
import com.trecapps.auth.models.TrecAuthentication;
import com.trecapps.pictures.models.PictureData;
import com.trecapps.pictures.services.PictureManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@RequestMapping("/profile")
@RestController
public class ProfileController {

    private static final List<String> imageTypes = Arrays.asList(
            "apng",
            "avif",
            "gif",
            "jpeg",
            "png",
            "svg",
            "webp"
    );

    @Autowired
    PictureManager pictureManager;

    @PostMapping(value = "/set", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<String> setPic(RequestEntity<String> request)
    {
        String contentType = request.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        if(null == contentType)
            return new ResponseEntity<>("Requires Content-Type header to be provided!", HttpStatus.BAD_REQUEST);
        String type = null;
        contentType = contentType.toLowerCase(Locale.ROOT);
        for(String imageType: imageTypes)
        {
            if(contentType.contains(imageType))
            {
                type = imageType;
                break;
            }
        }

        if(null == type)
            return new ResponseEntity<>("Requires Content-Type header to have 'apng' 'avif' 'gif' 'jpeg' 'png' 'svg' 'webp' ''!", HttpStatus.BAD_REQUEST);

        TrecAuthentication trecAuth = (TrecAuthentication) SecurityContextHolder.getContext().getAuthentication();
        String id = pictureManager.addNewPicture(
                trecAuth.getAccount().getId(),                              // Id if the user adding the picture
                request.getHeaders().getFirst("Picture-Name"),   // Name fo he picture (if provided)
                type,                                                       // Type of image we're dealing with
                request.getBody(),                                          // The image data in base64 format
                true);                                              // It's the profile pic so might as well be public


        String[] results = pictureManager.setProfile(trecAuth.getAccount().getId(), id).split("[:]");
        return new ResponseEntity<>(results[1], HttpStatus.valueOf(results[0]));
    }

    @GetMapping("/{userId}")
    ResponseEntity<PictureData> getProfile(@PathVariable("userId")String userId)
    {
        PictureData data = pictureManager.getProfile(userId);
        return new ResponseEntity<>(data, data.getError() == null ? HttpStatus.OK : HttpStatus.NOT_FOUND);
    }
}
