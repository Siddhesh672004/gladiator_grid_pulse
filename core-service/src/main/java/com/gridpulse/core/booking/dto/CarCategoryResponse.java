package com.gridpulse.core.booking.dto;

// imageUrl is empty when cloudinary is not set up, the page then shows the name only
public record CarCategoryResponse(String name, String imageUrl) {
}
