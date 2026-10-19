package com.gridpulse.core.feedback.data;

import java.time.LocalDate;

import com.gridpulse.core.account.data.User;
import com.gridpulse.core.session.data.RaceSession;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long feedbackId;

    @Column(length = 1000)
    private String feedbackText;

    private Integer rating;

    private LocalDate date;

    // the ai label, null on old rows (the api shows that as NOT_ANALYSED)
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Sentiment sentiment;

    // the ai found an injury / accident / safety problem in the text
    @Column(columnDefinition = "boolean default false")
    private boolean urgent;

    @ManyToOne
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "sessionId")
    private RaceSession session;

    // sentiment + urgent are set later by FeedbackAiService
    public Feedback(String feedbackText, Integer rating, LocalDate date, User user, RaceSession session) {
        this.feedbackText = feedbackText;
        this.rating = rating;
        this.date = date;
        this.user = user;
        this.session = session;
    }

    public Feedback(Long feedbackId, String feedbackText, Integer rating, LocalDate date, User user,
            RaceSession session) {
        this(feedbackText, rating, date, user, session);
        this.feedbackId = feedbackId;
    }
}
