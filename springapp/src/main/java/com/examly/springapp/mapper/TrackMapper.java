package com.examly.springapp.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.examly.springapp.dto.request.TrackRequest;
import com.examly.springapp.dto.response.TrackResponse;
import com.examly.springapp.model.RaceTrack;
import com.examly.springapp.repository.TrackOffer;

@Component
public class TrackMapper {

    public RaceTrack toEntity(TrackRequest request) {
        RaceTrack track = new RaceTrack();
        updateEntity(track, request);
        return track;
    }

    public void updateEntity(RaceTrack track, TrackRequest request) {
        track.setTrackName(request.trackName().trim());
        track.setDescription(request.description());
        track.setLocation(request.location());
        track.setTrackCategory(request.trackCategory());
        track.setIsIndoor(request.isIndoor());
        track.setOpenTime(request.openTime());
        track.setCloseTime(request.closeTime());
        track.setFacilities(request.facilities());
        track.setImageUrls(new ArrayList<>(request.imageUrls()));
    }

    // offer = sessions a driver can still book + the cheapest price, null = none
    public TrackResponse toResponse(RaceTrack track, TrackOffer offer) {
        // no offer -> 0 sessions and no price
        return new TrackResponse(track.getTrackId(), track.getTrackName(), track.getDescription(),
                track.getLocation(), track.getTrackCategory(), track.getIsIndoor(),
                track.getOpenTime(), track.getCloseTime(), track.getFacilities(), track.isActive(),
                pictures(track),
                Optional.ofNullable(offer).map(TrackOffer::getSessions).orElse(0L),
                Optional.ofNullable(offer).map(TrackOffer::getFromPrice).orElse(null));
    }

    // a track saved before pictures existed may come back from the db without a list at all
    private List<String> pictures(RaceTrack track) {
        // no list -> an empty one, else a read-only copy
        return Optional.ofNullable(track.getImageUrls())
                .map(List::copyOf)
                .orElse(List.of());
    }
}
