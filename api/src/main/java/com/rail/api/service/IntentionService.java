package com.rail.api.service;

import com.rail.api.component.DtoMapper;
import com.rail.api.dto.GoalDto;
import com.rail.api.dto.GoalTargetDto;
import com.rail.api.dto.IntentionDto;
import com.rail.api.entity.GoalStatus;
import com.rail.api.entity.GoalType;
import com.rail.api.entity.Intention;
import com.rail.api.entity.IntentionProposal;
import com.rail.api.entity.IntentionProposalStatus;
import com.rail.api.entity.IntentionStatus;
import com.rail.api.entity.IntentionType;
import com.rail.api.entity.TaskStatus;
import com.rail.api.entity.User;
import com.rail.api.event.IntentionConfirmedEvent;
import com.rail.api.repository.DailyScheduleEntryRepository;
import com.rail.api.repository.GoalRepository;
import com.rail.api.repository.GoalTargetRepository;
import com.rail.api.repository.IntentionProposalRepository;
import com.rail.api.repository.IntentionRepository;
import com.rail.api.repository.MilestoneRepository;
import com.rail.api.repository.TaskTargetRepository;
import com.rail.api.repository.UserSchedulingProfileRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class IntentionService {

    private static final List<IntentionStatus> ACTIVE_STATUSES = List.of(
        IntentionStatus.ACTIVE,
        IntentionStatus.PAUSED
    );

    private final IntentionRepository intentionRepository;
    private final IntentionProposalRepository proposalRepository;
    private final GoalRepository goalRepository;
    private final GoalTargetRepository goalTargetRepository;
    private final MilestoneRepository milestoneRepository;
    private final TaskTargetRepository taskTargetRepository;
    private final DailyScheduleEntryRepository dailyScheduleEntryRepository;
    private final UserSchedulingProfileRepository profileRepository;
    private final IntentionGenerationService generationService;
    private final StreakService streakService;
    private final DtoMapper dtoMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public IntentionDto confirmProposal(User user, UUID proposalId) {
        IntentionProposal proposal = proposalRepository
            .findByPidAndOwner(proposalId, user)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Proposal not found"
                )
            );

        if (proposal.getStatus() != IntentionProposalStatus.REFINING) {
            throw new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Proposal has already been " +
                    proposal.getStatus().name().toLowerCase()
            );
        }

        if (proposal.getSynthesis() == null) {
            throw new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Proposal has no synthesis yet — continue the conversation with Connie"
            );
        }

        Intention intention = generationService.generateFromProposal(
            user,
            proposal
        );
        var goalPid = goalRepository
            .findByIntentionAndStatus(intention, GoalStatus.ACTIVE)
            .map(g -> g.getPid())
            .orElse(null);
        eventPublisher.publishEvent(
            new IntentionConfirmedEvent(
                user,
                intention.getTitle(),
                intention.getPid(),
                goalPid,
                proposal.getChat().getEntityType(),
                proposal.getChat().getEntityId()
            )
        );
        return toDto(intention, true);
    }

    @Transactional(readOnly = true)
    public List<IntentionDto> list(User user) {
        return intentionRepository
            .findByOwnerAndStatusInOrderByCreatedAtDesc(user, ACTIVE_STATUSES)
            .stream()
            .map(i -> toDto(i, true))
            .toList();
    }

    @Transactional(readOnly = true)
    public IntentionDto get(User user, UUID pid) {
        return toDto(resolveIntention(user, pid), true);
    }

    @Transactional
    public IntentionDto updateStatus(
        User user,
        UUID pid,
        IntentionStatus newStatus
    ) {
        Intention intention = resolveIntention(user, pid);
        validateStatusTransition(intention, newStatus);
        intention.setStatus(newStatus);
        if (newStatus == IntentionStatus.COMPLETED) {
            intention.setCompletedAt(Instant.now());
        }
        intentionRepository.save(intention);
        return toDto(intention, true);
    }

    private void validateStatusTransition(
        Intention intention,
        IntentionStatus newStatus
    ) {
        if (
            newStatus == IntentionStatus.COMPLETED &&
            intention.getType() == IntentionType.UNBOUNDED
        ) {
            throw new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "UNBOUNDED intentions cannot be completed"
            );
        }
        if (intention.getStatus() == IntentionStatus.ABANDONED) {
            throw new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Abandoned intentions cannot be updated"
            );
        }
    }

    private Intention resolveIntention(User user, UUID pid) {
        return intentionRepository
            .findByPidAndOwner(pid, user)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Intention not found"
                )
            );
    }

    private IntentionDto toDto(Intention intention, boolean includeGoal) {
        GoalDto goalDto = null;
        if (includeGoal) {
            goalDto = goalRepository
                .findByIntentionAndStatus(intention, GoalStatus.ACTIVE)
                .map(goal -> {
                    var user = intention.getOwner();
                    var milestones = milestoneRepository.findByGoalOrderByPosition(goal);
                    var today = profileRepository.findByUser(user)
                        .map(p -> LocalDate.now(ZoneId.of(p.getTimezone())))
                        .orElseGet(LocalDate::now);
                    var todaysTasks = dailyScheduleEntryRepository
                        .findTaskEntriesByUserAndDateAndGoal(user, today, goal)
                        .stream()
                        .map(e -> e.getTask())
                        .toList();
                    var target = goalTargetRepository
                        .findByGoal(goal)
                        .map(gt -> {
                            var current = taskTargetRepository.sumActualValueByGoalAndTaskStatus(
                                goal, TaskStatus.DONE
                            );
                            return new GoalTargetDto(gt.getTargetValue(), current, gt.getUnit());
                        })
                        .orElse(null);
                    var habitStats = (goal.getType() == GoalType.HABIT || goal.getType() == GoalType.ABSTINENCE)
                        ? streakService.computeStats(goal)
                        : null;
                    return dtoMapper.toGoalDto(
                        goal,
                        intention.getTitle(),
                        target,
                        habitStats,
                        milestones,
                        todaysTasks
                    );
                })
                .orElse(null);
        }
        return dtoMapper.toIntentionDto(intention, goalDto);
    }
}
