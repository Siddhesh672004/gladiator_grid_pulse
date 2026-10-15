package com.examly.springapp.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.examly.springapp.config.CacheConfig;
import com.examly.springapp.dto.request.TrackRequest;
import com.examly.springapp.dto.response.TrackResponse;
import com.examly.springapp.exceptions.ConflictException;
import com.examly.springapp.exceptions.DuplicateTrackException;
import com.examly.springapp.exceptions.ResourceNotFoundException;
import com.examly.springapp.exceptions.TrackDeletionException;
import com.examly.springapp.mapper.TrackMapper;
import com.examly.springapp.model.CompetitionPhase;
import com.examly.springapp.model.RaceSession;
import com.examly.springapp.model.RaceTrack;
import com.examly.springapp.model.SessionStatus;
import com.examly.springapp.repository.RaceSessionRepo;
import com.examly.springapp.repository.RaceTrackRepo;
import com.examly.springapp.repository.TrackOffer;
import com.examly.springapp.validation.ValidationRules;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RaceTrackServiceImpl implements RaceTrackService {

    private static final List<String> TRACK_CATEGORIES = List.of("Circuit", "Karting", "Rally", "Drag Strip");

    private final RaceTrackRepo raceTrackRepo;
    private final RaceSessionRepo raceSessionRepo;
    private final TrackMapper trackMapper;
    private final CloudinaryService cloudinaryService;

    @Override
    @CacheEvict(cacheNames = { CacheConfig.TRACKS, CacheConfig.TRACK }, allEntries = true)
    public TrackResponse addTrack(TrackRequest request) {
        RaceTrack track = trackMapper.toEntity(request);
        checkTrackRules(track);
        checkNameFree(track);
        RaceTrack saved = saveTrack(track);
        log.info("Track added: {} (id {})", saved.getTrackName(), saved.getTrackId());
        return toResponse(saved);
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.TRACK, key = "#trackId")
    public TrackResponse getTrackById(Long trackId) {
        return toResponse(findTrack(trackId));
    }

    // every page with tracks asks for this list, redis answers for 30 s
    @Override
    @Cacheable(cacheNames = CacheConfig.TRACKS)
    public List<TrackResponse> getAllTracks() {
        Map<Long, TrackOffer> offers = offers();
        return raceTrackRepo.findAll().stream()
                .map(track -> trackMapper.toResponse(track, offers.get(track.getTrackId())))
                .toList();
    }

    @Override
    @CacheEvict(cacheNames = { CacheConfig.TRACKS, CacheConfig.TRACK }, allEntries = true)
    public TrackResponse updateTrack(Long trackId, TrackRequest request) {
        RaceTrack existing = findTrack(trackId);
        trackMapper.updateEntity(existing, request);
        checkTrackRules(existing);
        checkNameFree(existing);
        RaceTrack saved = saveTrack(existing);
        log.info("Track updated: {} (id {})", saved.getTrackName(), trackId);
        return toResponse(saved);
    }

    // a track with sessions has history, so it is only deactivated
    @Override
    @CacheEvict(cacheNames = { CacheConfig.TRACKS, CacheConfig.TRACK }, allEntries = true)
    public TrackResponse deleteTrack(Long trackId) {
        RaceTrack track = findTrack(trackId);
        // only a track that never had a session can go
        Optional.of(track)
                .filter(unused -> !raceSessionRepo.existsByTrackTrackId(trackId))
                .orElseThrow(() -> new TrackDeletionException("Track '" + track.getTrackName()
                        + "' has sessions and cannot be deleted - deactivate it instead"));
        raceTrackRepo.delete(track);
        log.info("Track deleted: {} (id {})", track.getTrackName(), trackId);
        return trackMapper.toResponse(track, null);
    }

    @Override
    @CacheEvict(cacheNames = { CacheConfig.TRACKS, CacheConfig.TRACK }, allEntries = true)
    public TrackResponse deactivateTrack(Long trackId) {
        RaceTrack track = findTrack(trackId);
        // scheduled or live sessions have to be cancelled or finished first
        Optional.of(track)
                .filter(free -> !raceSessionRepo.existsByTrackTrackIdAndStatusIn(trackId,
                        List.of(SessionStatus.SCHEDULED, SessionStatus.LIVE)))
                .orElseThrow(() -> new ConflictException("Track '" + track.getTrackName()
                        + "' still has upcoming sessions - cancel or finish them first"));
        return setActive(track, false);
    }

    @Override
    @CacheEvict(cacheNames = { CacheConfig.TRACKS, CacheConfig.TRACK }, allEntries = true)
    public TrackResponse activateTrack(Long trackId) {
        return setActive(findTrack(trackId), true);
    }

    private TrackResponse setActive(RaceTrack track, boolean active) {
        track.setActive(active);
        RaceTrack saved = raceTrackRepo.save(track);
        log.info("Track {} (id {}) is now {}", saved.getTrackName(), saved.getTrackId(),
                active ? "active" : "inactive");
        return toResponse(saved);
    }

    private TrackResponse toResponse(RaceTrack track) {
        return trackMapper.toResponse(track, offers().get(track.getTrackId()));
    }

    // bookable sessions per track in one grouped query, same rule as RaceSession.isBookable()
    private Map<Long, TrackOffer> offers() {
        LocalDateTime now = RaceSession.now();
        List<TrackOffer> rows = raceSessionRepo.findBookablePerTrack(SessionStatus.SCHEDULED,
                CompetitionPhase.RACING, now.toLocalDate(), now.toLocalTime());
        // track id -> its offer, a later row for the same track wins
        return rows.stream()
                .collect(Collectors.toMap(TrackOffer::getTrackId, offer -> offer, (first, later) -> later));
    }

    private RaceTrack findTrack(Long trackId) {
        return raceTrackRepo.findById(trackId)
                .orElseThrow(() -> new ResourceNotFoundException("Track with ID: " + trackId + " not found."));
    }

    private RaceTrack saveTrack(RaceTrack track) {
        try {
            return raceTrackRepo.save(track);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateTrackException("Track with name '" + track.getTrackName() + "' already exists", e);
        }
    }

    // the same name on another track -> 409 (a track may keep its own name on edit)
    private void checkNameFree(RaceTrack track) {
        // found under another id = the name is taken
        raceTrackRepo.findByTrackNameIgnoreCase(track.getTrackName())
                .filter(sameName -> !sameName.getTrackId().equals(track.getTrackId()))
                .ifPresent(sameName -> {
                    throw new DuplicateTrackException("Track with name '" + track.getTrackName() + "' already exists");
                });
    }

    private void checkTrackRules(RaceTrack track) {
        track.setTrackCategory(allowedValue(track.getTrackCategory()));

        // closing time has to come after opening time
        Optional.of(track)
                .filter(hours -> hours.getCloseTime().isAfter(hours.getOpenTime()))
                .orElseThrow(() -> new IllegalArgumentException("Closing Time must be after Opening Time"));

        // the first picture that was not uploaded through us stops the save (a missing link too)
        track.getImageUrls().stream()
                .filter(url -> !cloudinaryService.isOurImage(url))
                .map(url -> ValidationRules.PICTURE_NOT_OURS)
                .findFirst()
                .ifPresent(message -> {
                    throw new IllegalArgumentException(message);
                });
    }

    // "karting" is stored as "Karting", anything not in the list is refused
    private String allowedValue(String typed) {
        // the list's own spelling is what gets saved
        return TRACK_CATEGORIES.stream()
                .filter(allowed -> allowed.equalsIgnoreCase(typed.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Track Category must be one of " + TRACK_CATEGORIES));
    }
}
