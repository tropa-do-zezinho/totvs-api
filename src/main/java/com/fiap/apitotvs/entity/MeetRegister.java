package com.fiap.apitotvs.entity;

import com.fiap.apitotvs.enums.MeetRequestStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "meet_register")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"user", "insightResult"})
public class MeetRegister {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Blob URL is required")
    @Column(name = "blob_url", nullable = false, length = 2048)
    private String blobUrl;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "request_id", nullable = false, unique = true)
    private String requestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private MeetRequestStatus status = MeetRequestStatus.CRIADO;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(mappedBy = "meetRegister", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private WorkerInsightResult insightResult;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = MeetRequestStatus.CRIADO;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void markAnalisando() {
        this.status = MeetRequestStatus.ANALISANDO;
        this.errorMessage = null;
    }

    public void markProcessado() {
        this.status = MeetRequestStatus.PROCESSADO;
        this.errorMessage = null;
    }

    public void markFalha(String message) {
        this.status = MeetRequestStatus.FALHA;
        this.errorMessage = message;
    }
}
