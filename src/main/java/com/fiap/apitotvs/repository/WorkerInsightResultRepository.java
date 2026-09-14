package com.fiap.apitotvs.repository;

import com.fiap.apitotvs.entity.WorkerInsightResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkerInsightResultRepository extends JpaRepository<WorkerInsightResult, Long> {

    Optional<WorkerInsightResult> findByRequestId(String requestId);

    @Query("""
            select r from WorkerInsightResult r
            join fetch r.meetRegister m
            where m.user.id = :userId
            order by r.receivedAt desc
            """)
    List<WorkerInsightResult> findAllByUserId(@Param("userId") Long userId);

    @Query("""
            select r from WorkerInsightResult r
            join fetch r.meetRegister m
            left join fetch r.meetings
            where r.requestId = :requestId and m.user.id = :userId
            """)
    Optional<WorkerInsightResult> findDetailedByRequestIdAndUserId(
            @Param("requestId") String requestId,
            @Param("userId") Long userId
    );
}
