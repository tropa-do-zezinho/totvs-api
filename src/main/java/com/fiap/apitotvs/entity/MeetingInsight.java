package com.fiap.apitotvs.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(
        name = "meeting_insight",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_meeting_insight_request_meeting",
                columnNames = {"request_id", "id_meeting"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "insightResult")
public class MeetingInsight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private String requestId;

    @Column(name = "id_meeting", nullable = false)
    private Long idMeeting;

    @Column(name = "status_analise", length = 64)
    private String statusAnalise;

    @Column(name = "modelo", length = 128)
    private String modelo;

    @Column(name = "versao_prompt", length = 128)
    private String versaoPrompt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payloadJson;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insight_result_id", nullable = false)
    private WorkerInsightResult insightResult;
}
