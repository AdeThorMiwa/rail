package com.rail.api.connie;

import com.rail.api.entity.User;
import com.rail.api.entity.UserConnieLog;
import com.rail.api.entity.UserConnieLogType;
import com.rail.api.repository.UserConnieLogRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConnieProfileService {

    @Value("${rail.connie.pattern-analysis.max-logs:14}")
    private int maxLogs;

    @Value("${rail.connie.pattern-analysis.merge-batch:8}")
    private int mergeBatch;

    private final UserConnieLogRepository logRepository;
    private final AlgorithmicPatternAnalyzer algorithmicAnalyzer;
    private final ConniePatternAnalystClient analystClient;

    @Transactional
    public void runDailyAnalysis(User user) {
        log.info("[connie-profile] running daily analysis for user {}", user.getPid());

        AlgorithmicMetrics metrics = algorithmicAnalyzer.analyze(user);
        String algorithmicSummary = algorithmicAnalyzer.format(metrics);

        String llmObservations = analystClient.enrichObservations(
            algorithmicSummary, user.getDisplayName()
        );

        String observedPatterns = llmObservations != null
            ? "%s\n\nAlgorithmic metrics:\n%s".formatted(llmObservations.strip(), algorithmicSummary)
            : algorithmicSummary;

        LocalDate today = LocalDate.now();
        UserConnieLog entry = UserConnieLog.builder()
            .user(user)
            .type(UserConnieLogType.ANALYSIS)
            .periodStart(today)
            .periodEnd(today)
            .observedPatterns(observedPatterns)
            .build();

        logRepository.saveAndFlush(entry);
        log.info("[connie-profile] saved analysis entry for user {}", user.getPid());

        pruneIfNeeded(user);
    }

    @Transactional
    public void appendStatedPreference(User user, String preference) {
        String timestamped = "[%s] %s".formatted(
            LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE), preference.strip()
        );

        UserConnieLog latest = logRepository.findTopByUserAndTypeOrderByCreatedAtDesc(user, UserConnieLogType.ANALYSIS)
            .orElseGet(() -> {
                LocalDate today = LocalDate.now();
                return logRepository.save(UserConnieLog.builder()
                    .user(user)
                    .type(UserConnieLogType.ANALYSIS)
                    .periodStart(today)
                    .periodEnd(today)
                    .build());
            });

        String existing = latest.getStatedPreferences();
        latest.setStatedPreferences(
            existing == null || existing.isBlank()
                ? timestamped
                : existing + "\n" + timestamped
        );
        logRepository.save(latest);
        log.info("[connie-profile] appended stated preference for user {}", user.getPid());
    }

    private void pruneIfNeeded(User user) {
        long count = logRepository.countByUser(user);
        if (count <= maxLogs) return;

        log.info("[connie-profile] pruning — count={} for user {}", count, user.getPid());
        List<UserConnieLog> oldest = logRepository.findOldestN(user, mergeBatch);
        if (oldest.size() < mergeBatch) return;

        List<String> patterns = oldest.stream()
            .map(UserConnieLog::getObservedPatterns)
            .collect(Collectors.toList());

        List<String> preferences = oldest.stream()
            .map(UserConnieLog::getStatedPreferences)
            .collect(Collectors.toList());

        String merged = analystClient.mergeIntoEvolutionary(patterns, preferences, user.getDisplayName());

        LocalDate periodStart = oldest.get(0).getPeriodStart();
        LocalDate periodEnd = oldest.get(oldest.size() - 1).getPeriodEnd();

        String mergedPreferences = oldest.stream()
            .map(UserConnieLog::getStatedPreferences)
            .filter(s -> s != null && !s.isBlank())
            .collect(Collectors.joining("\n"));

        UserConnieLog evolutionary = UserConnieLog.builder()
            .user(user)
            .type(UserConnieLogType.EVOLUTIONARY)
            .periodStart(periodStart)
            .periodEnd(periodEnd)
            .observedPatterns(merged != null ? merged : patterns.stream()
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining("\n---\n")))
            .statedPreferences(mergedPreferences.isBlank() ? null : mergedPreferences)
            .mergedCount(oldest.size())
            .build();

        logRepository.save(evolutionary);
        logRepository.deleteAll(oldest);
        log.info("[connie-profile] merged {} entries into evolutionary record for user {}", oldest.size(), user.getPid());
    }
}
