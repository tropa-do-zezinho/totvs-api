package com.fiap.apitotvs.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "meet_register")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MeetRegister {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Blob URL is required")
    private String blobUrl;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
