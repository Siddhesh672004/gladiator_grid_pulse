package com.gridpulse.common.event;

// the kafka topics, one per business area. the message key keeps one account / booking in order
public final class Topics {

    public static final String ACCOUNT = "gridpulse.account";
    public static final String BOOKING = "gridpulse.booking";
    public static final String PAYMENT = "gridpulse.payment";
    public static final String RACE = "gridpulse.race";
    public static final String FEEDBACK = "gridpulse.feedback";

    private Topics() {
    }
}
