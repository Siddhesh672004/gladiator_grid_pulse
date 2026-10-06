package com.examly.springapp.repository;

// one row of RaceSessionRepo.findBookablePerTrack: sessions a driver can still book + the cheapest price
public interface TrackOffer {

    Long getTrackId();

    Long getSessions();

    Double getFromPrice();
}
