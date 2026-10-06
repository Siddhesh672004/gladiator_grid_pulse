package com.examly.springapp.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.examly.springapp.config.properties.CloudinaryProperties;
import com.examly.springapp.exceptions.GatewayException;
import com.examly.springapp.validation.ValidationRules;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// signed upload to cloudinary with plain HttpClient (no cloudinary library). the browser loads pictures from cloudinary directly
@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private static final String UPLOAD_URL = "https://api.cloudinary.com/v1_1/";
    private static final String PICTURE_URL = "https://res.cloudinary.com/";
    private static final String NOT_SAVED = "The picture could not be saved right now. Please try again later";

    private final CloudinaryProperties cloudinary;
    private final ObjectMapper objectMapper;

    private HttpClient httpClient;

    @PostConstruct
    public void init() {
        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(cloudinary.timeoutSeconds())).build();
        // no keys: the server still starts, the log says why no picture can be uploaded
        Optional.of(cloudinary)
                .filter(keys -> !keys.isConfigured())
                .ifPresent(keys -> log.warn(
                        "CLOUDINARY_* keys are not set in secrets.properties - pictures cannot be uploaded"));
    }

    @Override
    public String upload(MultipartFile file, String folder) {
        checkPicture(file);
        // no keys in secrets.properties -> 503
        Optional.of(cloudinary)
                .filter(CloudinaryProperties::isConfigured)
                .orElseThrow(() -> new GatewayException("Picture upload is not set up yet. Please try again later"));
        try {
            HttpRequest request = buildUpload(file, cloudinary.folder() + "/" + folder);
            return readLink(httpClient.send(request, HttpResponse.BodyHandlers.ofString()));
        } catch (IOException e) {
            log.warn("Cloudinary upload failed: {}", e.getMessage());
            throw new GatewayException(NOT_SAVED, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GatewayException(NOT_SAVED, e);
        }
    }

    // without keys nothing can be uploaded here, so a cloudinary link can only be one that was saved before
    @Override
    public boolean isOurImage(String url) {
        // no link or a link with a space is never ours
        return Optional.ofNullable(url)
                .filter(link -> link.chars().noneMatch(Character::isWhitespace))
                .map(link -> link.startsWith(cloudinary.isConfigured() ? picturePrefix() : PICTURE_URL))
                .orElse(false);
    }

    @Override
    public String fixedImageUrl(String publicId) {
        return cloudinary.isConfigured() ? picturePrefix() + publicId : null;
    }

    private void checkPicture(MultipartFile file) {
        // nothing chosen, then the type, then the size
        MultipartFile picture = Optional.ofNullable(file)
                .filter(chosen -> !chosen.isEmpty())
                .orElseThrow(() -> new IllegalArgumentException(ValidationRules.PICTURE_REQUIRED));
        Optional.of(picture)
                .filter(chosen -> ValidationRules.PICTURE_TYPES.contains(chosen.getContentType()))
                .orElseThrow(() -> new IllegalArgumentException(ValidationRules.PICTURE_TYPE));
        Optional.of(picture)
                .filter(chosen -> chosen.getSize() <= ValidationRules.PICTURE_MAX_BYTES)
                .orElseThrow(() -> new IllegalArgumentException(ValidationRules.PICTURE_TOO_BIG));
    }

    private HttpRequest buildUpload(MultipartFile file, String folder) throws IOException {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        // signature = sha-1 of the signed fields in a-z order + the api secret
        String signature = sha1("folder=" + folder + "&timestamp=" + timestamp + cloudinary.apiSecret());

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("file", "data:" + file.getContentType() + ";base64,"
                + Base64.getEncoder().encodeToString(file.getBytes()));
        fields.put("folder", folder);
        fields.put("timestamp", timestamp);
        fields.put("api_key", cloudinary.apiKey());
        fields.put("signature", signature);

        return HttpRequest.newBuilder()
                .uri(URI.create(UPLOAD_URL + cloudinary.cloudName() + "/image/upload"))
                .timeout(Duration.ofSeconds(cloudinary.timeoutSeconds()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody(fields)))
                .build();
    }

    // 400 = cloudinary could not read the file as a picture, anything else = our problem (keys, cloudinary down)
    private String readLink(HttpResponse<String> response) throws IOException {
        JsonNode body = objectMapper.readTree(response.body());
        // 200 -> the link of the saved picture, anything else is refused
        return Optional.of(response)
                .filter(answer -> answer.statusCode() == 200)
                .map(answer -> body.path("secure_url").asText())
                .orElseThrow(() -> refused(response.statusCode(), body));
    }

    private RuntimeException refused(int status, JsonNode body) {
        log.warn("Cloudinary answered HTTP {}: {}", status, body.path("error").path("message").asText());
        return status == 400
                ? new IllegalArgumentException(ValidationRules.PICTURE_UNREADABLE)
                : new GatewayException(NOT_SAVED);
    }

    private String formBody(Map<String, String> fields) {
        return fields.entrySet().stream()
                .map(field -> field.getKey() + "=" + URLEncoder.encode(field.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    private String sha1(String text) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-1").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String picturePrefix() {
        return PICTURE_URL + cloudinary.cloudName() + "/image/upload/";
    }
}
