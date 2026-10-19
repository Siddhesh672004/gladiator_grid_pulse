package com.gridpulse.core.track.data;

// one row of a per-group count, e.g. "Karting" -> 4 tracks
public interface LabelAmount {

    String getLabel();

    Number getAmount();
}
