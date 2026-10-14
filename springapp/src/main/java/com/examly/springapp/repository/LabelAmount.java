package com.examly.springapp.repository;

// one row of a per-group count, e.g. "Karting" -> 4 tracks
public interface LabelAmount {

    String getLabel();

    Number getAmount();
}
