package com.lorofy.server.features.focus.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "app_violations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "focus_session_id", nullable = false)
    private FocusSession focusSession;

    @Column(name = "app_identifier", length = 255)
    private String appIdentifier;

    @Column(name = "app_name", length = 100)
    private String appName;

    @Column(name = "violation_type", nullable = false, length = 30)
    @Builder.Default
    private String violationType = "APP_OPENED";

    @Column(name = "attempted_at", insertable = false, updatable = false)
    private OffsetDateTime attemptedAt;
}
