package com.fiap.apitotvs.repository;

import com.fiap.apitotvs.entity.MeetRegister;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeetRegisterRepository extends JpaRepository<MeetRegister, Long> {
    List<MeetRegister> findAllByUser_Id(Long id);

    Page<MeetRegister> findAllByUser_Id(Long id, Pageable pageable);
}
