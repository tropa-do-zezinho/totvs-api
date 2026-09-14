package com.fiap.apitotvs.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "worker_insight_result")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"meetRegister", "meetings"})
public class WorkerInsightResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, unique = true)
    private String requestId;

    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;

    @Column(name = "schema_version", nullable = false, length = 32)
    private String schemaVersion;

    @Column(name = "worker_status", nullable = false, length = 64)
    private String workerStatus;

    @Column(name = "sent_at_utc")
    private Instant sentAtUtc;

    @Column(name = "insights_schema_version", length = 32)
    private String insightsSchemaVersion;

    @Column(name = "gerado_em_utc")
    private Instant geradoEmUtc;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resumo_json", columnDefinition = "jsonb")
    private Map<String, Object> resumoJson;

    @Column(name = "reunioes_recebidas", nullable = false)
    private Integer reunioesRecebidas = 0;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meet_register_id", nullable = false, unique = true)
    private MeetRegister meetRegister;

    @OneToMany(mappedBy = "insightResult", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MeetingInsight> meetings = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
        if (reunioesRecebidas == null) {
            reunioesRecebidas = 0;
        }
    }

    public void replaceMeetings(List<MeetingInsight> newMeetings) {
        meetings.clear();
        if (newMeetings == null) {
            return;
        }
        for (MeetingInsight meeting : newMeetings) {
            meeting.setInsightResult(this);
            meeting.setRequestId(this.requestId);
            meetings.add(meeting);
        }
        reunioesRecebidas = meetings.size();
    }
}
