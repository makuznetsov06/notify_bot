package home.makuznetsov.notifybot.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@NamedEntityGraph(
        name = "User.withReminders",
        attributeNodes = @NamedAttributeNode("reminders")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "reminders")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegram_user_id", nullable = false, unique = true)
    private Long telegramUserId;

    @Column(name = "username")
    private String username;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @CreationTimestamp
    @Column(name = "registered_at", nullable = false, updatable = false)
    private ZonedDateTime registeredAt;

    @Column(name = "language_code", length = 10)
    private String languageCode;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "timezone", length = 50)
    @Builder.Default
    private String timezone = "UTC";

    // Обратная связь с Reminder (OneToMany)
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private List<Reminder> reminders = new ArrayList<>();

    // Вспомогательный метод для добавления напоминания
    public void addReminder(Reminder reminder) {
        reminders.add(reminder);
        reminder.setUser(this);
    }
}