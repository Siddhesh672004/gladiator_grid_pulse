package com.gridpulse.common.inbox;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

// primary key of ProcessedEvent: one event, one consumer
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProcessedEventKey implements Serializable {

    private String eventId;
    private String consumer;
}
