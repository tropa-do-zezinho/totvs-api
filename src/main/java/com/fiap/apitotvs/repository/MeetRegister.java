package com.fiap.apitotvs.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MeetRegister extends JpaRepository<MeetRegister, Long> {
    MeetRegister findByUser_Id(Long id);

    Page<MeetRegister> findAllByUser_Id(Long id, Pageable pageable);
}
