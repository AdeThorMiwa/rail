package com.rail.api.service;

import com.rail.api.entity.Goal;
import com.rail.api.entity.GoalType;
import com.rail.api.entity.Milestone;
import com.rail.api.entity.MilestoneStatus;
import com.rail.api.entity.TaskStatus;
import com.rail.api.repository.GoalTargetRepository;
import com.rail.api.repository.MilestoneRepository;
import com.rail.api.repository.TaskRepository;
import com.rail.api.repository.TaskTargetRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaskCompletionService {

    private final TaskRepository taskRepository;
    private final TaskTargetRepository taskTargetRepository;
    private final GoalTargetRepository goalTargetRepository;
    private final MilestoneRepository milestoneRepository;

    @Lazy
    private final GoalService goalService;

    public void recalcMilestone(Milestone milestone) {
        List<com.rail.api.entity.Task> tasks = taskRepository.findByMilestone(
            milestone
        );
        boolean allDone = tasks
            .stream()
            .allMatch(t -> t.getStatus() == TaskStatus.DONE);
        if (!allDone) return;

        milestone.setStatus(MilestoneStatus.DONE);
        milestone.setAchievedAt(Instant.now());
        milestoneRepository.save(milestone);

        List<Milestone> ordered = milestoneRepository.findByGoalOrderByPosition(
            milestone.getGoal()
        );
        for (int i = 0; i < ordered.size(); i++) {
            if (
                ordered.get(i).getId().equals(milestone.getId()) &&
                i + 1 < ordered.size()
            ) {
                Milestone next = ordered.get(i + 1);
                if (next.getStatus() == MilestoneStatus.PENDING) {
                    next.setStatus(MilestoneStatus.IN_PROGRESS);
                    milestoneRepository.save(next);
                }
                break;
            }
        }
    }

    public void recalcGoalCompletion(Goal goal) {
        if (goal.getType() == GoalType.TASK) {
            boolean allDone = taskRepository
                .findByGoal(goal)
                .stream()
                .allMatch(t -> t.getStatus() == TaskStatus.DONE);
            if (!allDone) return;
        } else if (goal.getType() == GoalType.QUANTIFIED) {
            var gt = goalTargetRepository.findByGoal(goal).orElse(null);
            if (gt == null) return;
            var current =
                taskTargetRepository.sumActualValueByGoalAndTaskStatus(
                    goal,
                    TaskStatus.DONE
                );
            if (
                current == null || current.compareTo(gt.getTargetValue()) < 0
            ) return;
        } else {
            return;
        }
        goalService.finishGoal(goal, null);
    }
}
