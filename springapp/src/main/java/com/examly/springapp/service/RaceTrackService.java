package com.examly.springapp.service;

import java.util.List;

import com.examly.springapp.dto.request.TrackRequest;
import com.examly.springapp.dto.response.TrackResponse;

public interface RaceTrackService {

    TrackResponse addTrack(TrackRequest request);

    TrackResponse getTrackById(Long trackId);

    List<TrackResponse> getAllTracks();

    TrackResponse updateTrack(Long trackId, TrackRequest request);

    // only a track that never had a session, otherwise TrackDeletionException (409) -> deactivate it
    TrackResponse deleteTrack(Long trackId);

    // hidden from drivers. ConflictException (409) while it has upcoming or running sessions
    TrackResponse deactivateTrack(Long trackId);

    TrackResponse activateTrack(Long trackId);
}
