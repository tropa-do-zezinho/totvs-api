package com.fiap.apitotvs.repository;

import com.fiap.apitotvs.entity.MeetingInsight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MeetingInsightRepository extends JpaRepository<MeetingInsight, Long> {
    Optional<MeetingInsight> findByRequestIdAndIdMeeting(String requestId, Long idMeeting);
}
