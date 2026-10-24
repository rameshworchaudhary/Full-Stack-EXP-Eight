package com.college.lab.reliability.repository;

import com.college.lab.reliability.entity.EventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventLogRepository extends JpaRepository<EventLog, Long> {
    List<EventLog> findByEventIdOrderByAttemptNumberAsc(String eventId);
    List<EventLog> findAllByOrderByTimestampDesc();
}
