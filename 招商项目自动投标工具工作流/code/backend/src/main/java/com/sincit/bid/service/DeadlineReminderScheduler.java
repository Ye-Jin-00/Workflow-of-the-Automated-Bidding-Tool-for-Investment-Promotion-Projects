package com.sincit.bid.service;

import com.sincit.bid.domain.BidProject;
import com.sincit.bid.persistence.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 投标截止提醒：每 30 分钟扫描一次，3 天内或已逾期的未完成项目通知负责人与关注人。
 * dedupeKey 保证同一用户每天最多收到一次。
 */
@Component
public class DeadlineReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(DeadlineReminderScheduler.class);

    private final ProjectRepository projectRepository;
    private final NotificationService notificationService;

    public DeadlineReminderScheduler(ProjectRepository projectRepository, NotificationService notificationService) {
        this.projectRepository = projectRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelay = 1800000L, initialDelay = 60000L)
    public void scan() {
        int created = 0;
        for (BidProject project : projectRepository.findAll()) {
            if (project.getStatus().isFinished() || project.getDeadline() == null) {
                continue;
            }
            long days = ChronoUnit.DAYS.between(LocalDateTime.now(), project.getDeadline());
            if (days <= 3 && days >= -7) {
                notificationService.notifyDeadline(project, days);
                created++;
            }
        }
        if (created > 0) {
            log.info("deadline reminder scan finished, projects={}", created);
        }
    }
}
