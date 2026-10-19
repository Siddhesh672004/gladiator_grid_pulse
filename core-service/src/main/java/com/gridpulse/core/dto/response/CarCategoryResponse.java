package com.gridpulse.core.dto.response;

// imageUrl is empty when cloudinary is not set up, the page then shows the name only
public record CarCategoryResponse(String name, String imageUrl) {
}
