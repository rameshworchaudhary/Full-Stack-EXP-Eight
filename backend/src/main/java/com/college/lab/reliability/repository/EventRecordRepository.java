package com.college.lab.reliability.repository;

import com.college.lab.reliability.entity.EventRecord;
import com.college.lab.reliability.entity.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRecordRepository extends JpaRepository<EventRecord, Long> {
    Optional<EventRecord> findByEventId(String eventId);
    boolean existsByEventId(String eventId);
    List<EventRecord> findAllByOrderByCreatedAtDesc();
    long countByStatus(EventStatus status);
}
