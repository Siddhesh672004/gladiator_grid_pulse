package com.gridpulse.core.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.gridpulse.core.model.RaceTrack;

public interface RaceTrackRepo extends JpaRepository<RaceTrack, Long> {

    Optional<RaceTrack> findByTrackNameIgnoreCase(String trackName);

    @Query("select t.trackCategory as label, count(t) as amount from RaceTrack t group by t.trackCategory"
            + " order by t.trackCategory")
    List<LabelAmount> countPerCategory();
}
