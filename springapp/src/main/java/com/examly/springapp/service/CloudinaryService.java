package com.examly.springapp.service;

import org.springframework.web.multipart.MultipartFile;

// all picture storage goes through here, the db only keeps the link
public interface CloudinaryService {

    // returns the https link. bad file -> IllegalArgumentException (400), cloudinary problem -> GatewayException (503)
    String upload(MultipartFile file, String folder);

    // only links of our own cloudinary account are accepted
    boolean isOurImage(String url);

    String fixedImageUrl(String publicId);
}
