package com.gridpulse.ai.faq.data;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// id is NOT generated, it comes from faqs.json so re-seeding never makes duplicates
@Entity
@Table(name = "faqs")
@Getter
@Setter
@NoArgsConstructor
public class FaqEntity {

    @Id
    private Long id;

    private String category;

    @Column(length = 500)
    private String question;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String answer;

    // null when gemini was not configured at seeding time -> word-overlap search is used for this row
    @Convert(converter = FloatArrayConverter.class)
    @Column(columnDefinition = "LONGTEXT")
    private float[] embedding;

    public FaqEntity(Long id, String category, String question, String answer) {
        this.id = id;
        this.category = category;
        this.question = question;
        this.answer = answer;
    }

    public FaqEntity(Long id, String category, String question, String answer, float[] embedding) {
        this(id, category, question, answer);
        this.embedding = embedding;
    }
}
