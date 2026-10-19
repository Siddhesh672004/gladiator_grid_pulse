package com.gridpulse.core.track.service;

import java.util.List;

import com.gridpulse.core.track.dto.TrackRequest;
import com.gridpulse.core.track.dto.TrackResponse;

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
