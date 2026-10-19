package com.gridpulse.core.track.data;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// no price here on purpose, every session has its own price
@Entity
@Getter
@Setter
@NoArgsConstructor
public class RaceTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long trackId;

    @Column(unique = true, nullable = false, length = 100)
    private String trackName;

    @Column(length = 1000)
    private String description;

    private String location;

    private String trackCategory;

    // lombok makes getIsIndoor() for a Boolean
    private Boolean isIndoor;

    private LocalTime openTime;

    private LocalTime closeTime;

    @Column(length = 500)
    private String facilities;

    // false = switched off by the admin: hidden from drivers, no new sessions
    @Column(columnDefinition = "boolean default true")
    private boolean active = true;

    // 1 - 5 cloudinary links in ONE column (StringListConverter), the first one is the cover
    @Convert(converter = StringListConverter.class)
    @Column(length = 2000)
    private List<String> imageUrls = new ArrayList<>();

    public RaceTrack(String trackName, String description, String location, String trackCategory, Boolean isIndoor,
            LocalTime openTime, LocalTime closeTime, String facilities) {
        this.trackName = trackName;
        this.description = description;
        this.location = location;
        this.trackCategory = trackCategory;
        this.isIndoor = isIndoor;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.facilities = facilities;
    }

    public RaceTrack(Long trackId, String trackName, String description, String location, String trackCategory,
            Boolean isIndoor, LocalTime openTime, LocalTime closeTime, String facilities) {
        this(trackName, description, location, trackCategory, isIndoor, openTime, closeTime, facilities);
        this.trackId = trackId;
    }
}
