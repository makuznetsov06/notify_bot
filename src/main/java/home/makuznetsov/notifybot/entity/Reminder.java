package home.makuznetsov.notifybot.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;

@Entity
@Table(name = "reminder")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", nullable = false, length = 511)
    private String title;

    @Column(name = "scheduled_time", nullable = false)
    private ZonedDateTime scheduledTime;

    @Column(name = "recurrence_pattern", length = 50)
    private String recurrencePattern;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ReminderStatus status = ReminderStatus.SCHEDULED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "last_triggered_at")
    private ZonedDateTime lastTriggeredAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;
}