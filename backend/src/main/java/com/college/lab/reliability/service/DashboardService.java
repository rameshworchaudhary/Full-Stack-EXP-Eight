package com.college.lab.reliability.service;

import com.college.lab.reliability.dto.DashboardStatsResponse;
import com.college.lab.reliability.entity.EventStatus;
import com.college.lab.reliability.repository.DlqRecordRepository;
import com.college.lab.reliability.repository.EventRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final EventRecordRepository eventRecordRepository;
    private final DlqRecordRepository dlqRecordRepository;

    public DashboardService(EventRecordRepository eventRecordRepository, DlqRecordRepository dlqRecordRepository) {
        this.eventRecordRepository = eventRecordRepository;
        this.dlqRecordRepository = dlqRecordRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        long total = eventRecordRepository.count();
        long success = eventRecordRepository.countByStatus(EventStatus.SUCCESS);
        long failed = eventRecordRepository.countByStatus(EventStatus.FAILED);
        long retrying = eventRecordRepository.countByStatus(EventStatus.RETRYING);
        long duplicateIgnored = eventRecordRepository.countByStatus(EventStatus.DUPLICATE_IGNORED);
        long dlqCount = dlqRecordRepository.count();

        return new DashboardStatsResponse(
                total,
                success,
                failed,
                retrying,
                duplicateIgnored,
                dlqCount
        );
    }
}
