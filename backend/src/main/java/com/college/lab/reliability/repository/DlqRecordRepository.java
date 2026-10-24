package com.college.lab.reliability.repository;

import com.college.lab.reliability.entity.DlqRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DlqRecordRepository extends JpaRepository<DlqRecord, Long> {
    List<DlqRecord> findAllByOrderByReceivedAtDesc();
    Optional<DlqRecord> findByEventId(String eventId);
    long count();
}
