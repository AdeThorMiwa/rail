package com.rail.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rail.api.dto.CycleFocusDto;
import com.rail.api.dto.UserCycleDto;
import com.rail.api.entity.CycleFocus;
import com.rail.api.entity.CycleStatus;
import com.rail.api.entity.Goal;
import com.rail.api.entity.GoalType;
import com.rail.api.entity.OccurrenceStatus;
import com.rail.api.entity.TaskStatus;
import com.rail.api.entity.User;
import com.rail.api.entity.ChatEntityType;
import com.rail.api.entity.UserCycle;
import com.rail.api.intelligence.RetroAnalysis;
import com.rail.api.repository.CycleFocusRepository;
import com.rail.api.repository.GoalRepository;
import com.rail.api.repository.TaskOccurrenceRepository;
import com.rail.api.repository.TaskRepository;
import com.rail.api.repository.UserCycleRepository;
import com.rail.api.event.CyclePlanningOpenEvent;
import com.rail.api.event.CycleRetroOpenEvent;
import com.rail.api.repository.UserSchedulingProfileRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class CycleService {

    private final UserCycleRepository cycleRepository;
    private final CycleFocusRepository cycleFocusRepository;
    private final GoalRepository goalRepository;
    private final TaskRepository taskRepository;
    private final TaskOccurrenceRepository occurrenceRepository;
    private final UserSchedulingProfileRepository profileRepository;
    private final ChatService chatService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Value("${rail.cycle.max-days:14}")
    private int maxDays;

    @Transactional
    public UserCycleDto create(
        User owner,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime reviewTime,
        String title
    ) {
        boolean hasActive = !cycleRepository
            .findByOwnerAndStatusIn(
                owner,
                List.of(CycleStatus.ACTIVE, CycleStatus.PLANNED)
            )
            .isEmpty();
        if (hasActive) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Active or planned cycle already exists"
            );
        }
        if (endDate.isAfter(startDate.plusDays(maxDays))) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Cycle cannot exceed " + maxDays + " days"
            );
        }
        String resolvedTitle = (title != null && !title.isBlank())
            ? title.strip()
            : formatMonthDay(startDate) + " – " + formatMonthDay(endDate);
        LocalDate today = profileRepository.findByUser(owner)
            .map(p -> LocalDate.now(ZoneId.of(p.getTimezone())))
            .orElseGet(LocalDate::now);
        CycleStatus status = !startDate.isAfter(today)
            ? CycleStatus.ACTIVE
            : CycleStatus.PLANNED;
        LocalTime resolvedReviewTime =
            reviewTime != null ? reviewTime : LocalTime.of(19, 0);

        UserCycle cycle = cycleRepository.saveAndFlush(
            UserCycle.builder()
                .owner(owner)
                .title(resolvedTitle)
                .startDate(startDate)
                .endDate(endDate)
                .reviewTime(resolvedReviewTime)
                .status(status)
                .build()
        );

        chatService.getOrCreateChat(owner, ChatEntityType.CYCLE, cycle.getPid());
        eventPublisher.publishEvent(new CyclePlanningOpenEvent(owner, cycle.getPid()));
        return toDto(cycle);
    }

    public Optional<UserCycleDto> getActive(User owner) {
        return cycleRepository
            .findByOwnerAndStatusIn(
                owner,
                List.of(
                    CycleStatus.ACTIVE,
                    CycleStatus.PLANNED,
                    CycleStatus.IN_REVIEW
                )
            )
            .stream()
            .findFirst()
            .map(this::toDto);
    }

    @Transactional
    public void activatePlannedCycles() {
        for (var profile : profileRepository.findAll()) {
            LocalDate today = LocalDate.now(ZoneId.of(profile.getTimezone()));
            List<UserCycle> toActivate = cycleRepository
                .findByOwnerAndStatusIn(profile.getUser(), List.of(CycleStatus.PLANNED))
                .stream()
                .filter(c -> !c.getStartDate().isAfter(today))
                .toList();
            for (UserCycle cycle : toActivate) {
                cycle.setStatus(CycleStatus.ACTIVE);
                cycleRepository.save(cycle);
                log.info(
                    "Activated cycle {} for user {}",
                    cycle.getPid(),
                    cycle.getOwner().getPid()
                );
            }
        }
    }

    @Transactional
    public void transitionCyclesToReview() {
        for (var profile : profileRepository.findAll()) {
            ZoneId zone = ZoneId.of(profile.getTimezone());
            LocalDate today = LocalDate.now(zone);
            LocalTime now = LocalTime.now(zone);
            List<UserCycle> dueForReview = cycleRepository
                .findByOwnerAndStatusIn(profile.getUser(), List.of(CycleStatus.ACTIVE))
                .stream()
                .filter(c -> !c.getEndDate().isAfter(today) && !c.getReviewTime().isAfter(now))
                .toList();
            for (UserCycle cycle : dueForReview) {
                cycle.setStatus(CycleStatus.IN_REVIEW);
                RetroAnalysis stats = computeRetroStats(cycle);
                try {
                    cycle.setRetroAnalysis(objectMapper.writeValueAsString(stats));
                } catch (Exception e) {
                    log.warn(
                        "Failed to serialize retro stats for cycle {}: {}",
                        cycle.getPid(),
                        e.getMessage()
                    );
                }
                cycleRepository.save(cycle);
                log.info(
                    "Cycle {} transitioned to IN_REVIEW for user {}",
                    cycle.getPid(),
                    cycle.getOwner().getPid()
                );
                eventPublisher.publishEvent(new CycleRetroOpenEvent(cycle.getOwner(), cycle.getPid()));
            }
        }
    }

    @Transactional
    public void addFocusGoal(User user, UUID cyclePid, UUID goalPid) {
        UserCycle cycle = cycleRepository.findByPidAndOwner(cyclePid, user).orElse(null);
        if (cycle == null) {
            log.warn("addFocusGoal: cycle {} not found for user {}", cyclePid, user.getPid());
            return;
        }
        Goal goal = goalRepository.findByPidAndOwner(goalPid, user).orElse(null);
        if (goal == null) {
            log.warn("addFocusGoal: goal {} not found for user {}", goalPid, user.getPid());
            return;
        }
        boolean alreadyFocused = cycleFocusRepository
            .findByCycleOrderByPositionAsc(cycle)
            .stream()
            .anyMatch(f -> f.getGoal().getPid().equals(goalPid));
        if (alreadyFocused) return;

        int nextPosition = cycleFocusRepository.findByCycleOrderByPositionAsc(cycle).size();
        cycleFocusRepository.save(
            CycleFocus.builder()
                .cycle(cycle)
                .goal(goal)
                .position(nextPosition)
                .build()
        );
        log.info("addFocusGoal: added goal '{}' to cycle {}", goal.getTitle(), cyclePid);
    }

    public List<CycleFocusDto> getFocuses(User owner, UUID cyclePid) {
        return cycleRepository
            .findByPidAndOwner(cyclePid, owner)
            .map(cycle ->
                cycleFocusRepository
                    .findByCycleOrderByPositionAsc(cycle)
                    .stream()
                    .map(f ->
                        new CycleFocusDto(
                            f.getGoal().getPid(),
                            f.getGoal().getTitle(),
                            f.getGoal().getType(),
                            f.getPosition()
                        )
                    )
                    .toList()
            )
            .orElse(List.of());
    }

    private RetroAnalysis computeRetroStats(UserCycle cycle) {
        List<CycleFocus> focuses =
            cycleFocusRepository.findByCycleOrderByPositionAsc(cycle);
        List<Goal> focusGoals = focuses
            .stream()
            .map(CycleFocus::getGoal)
            .toList();

        List<RetroAnalysis.FocusGoalStats> focusStats = new ArrayList<>();
        List<RetroAnalysis.HabitStats> habitStats = new ArrayList<>();
        List<RetroAnalysis.AbstinenceStats> abstinenceStats = new ArrayList<>();

        for (Goal goal : focusGoals) {
            GoalType type = goal.getType();

            if (type == GoalType.HABIT) {
                var occs = occurrenceRepository.findByGoalsAndDateRange(
                    List.of(goal),
                    cycle.getStartDate(),
                    cycle.getEndDate()
                );
                int done = (int) occs
                    .stream()
                    .filter(o -> o.getStatus() == OccurrenceStatus.DONE)
                    .count();
                int skipped = (int) occs
                    .stream()
                    .filter(o -> o.getStatus() == OccurrenceStatus.SKIPPED)
                    .count();
                int missed = (int) occs
                    .stream()
                    .filter(o -> o.getStatus() == OccurrenceStatus.MISSED)
                    .count();
                int total = done + skipped + missed;
                double rate = total == 0 ? 0.0 : (double) done / total;
                habitStats.add(
                    new RetroAnalysis.HabitStats(
                        goal.getPid().toString(),
                        goal.getTitle(),
                        total,
                        done,
                        skipped,
                        missed,
                        rate
                    )
                );
            } else if (type == GoalType.ABSTINENCE) {
                var occs = occurrenceRepository.findByGoalsAndDateRange(
                    List.of(goal),
                    cycle.getStartDate(),
                    cycle.getEndDate()
                );
                long totalDays =
                    ChronoUnit.DAYS.between(
                        cycle.getStartDate(),
                        cycle.getEndDate()
                    ) + 1;
                int lapses = (int) occs
                    .stream()
                    .filter(o -> o.getStatus() == OccurrenceStatus.MISSED)
                    .count();
                int resisted = (int) (totalDays - lapses);
                double rate =
                    totalDays == 0
                        ? 1.0
                        : Math.max(
                              0.0,
                              (double) (totalDays - lapses) / totalDays
                          );
                abstinenceStats.add(
                    new RetroAnalysis.AbstinenceStats(
                        goal.getPid().toString(),
                        goal.getTitle(),
                        (int) totalDays,
                        resisted,
                        lapses,
                        rate
                    )
                );
            } else {
                var tasks = taskRepository.findByGoal(goal);
                int total = tasks.size();
                int completed = (int) tasks
                    .stream()
                    .filter(t -> t.getStatus() == TaskStatus.DONE)
                    .count();
                int skipped = (int) tasks
                    .stream()
                    .filter(t -> t.getStatus() == TaskStatus.SKIPPED)
                    .count();
                double rate = total == 0 ? 0.0 : (double) completed / total;
                focusStats.add(
                    new RetroAnalysis.FocusGoalStats(
                        goal.getPid().toString(),
                        goal.getTitle(),
                        type.name().toLowerCase(),
                        total,
                        completed,
                        skipped,
                        rate
                    )
                );
            }
        }

        return new RetroAnalysis(
            focusStats,
            habitStats,
            abstinenceStats,
            null,
            null,
            null
        );
    }

    private UserCycleDto toDto(UserCycle c) {
        return new UserCycleDto(
            c.getPid(),
            c.getTitle(),
            c.getStartDate(),
            c.getEndDate(),
            c.getReviewTime(),
            c.getStatus()
        );
    }

    private String formatMonthDay(LocalDate date) {
        return (
            date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) +
            " " +
            date.getDayOfMonth()
        );
    }
}
