package com.rail.api.service;

import com.rail.api.component.DtoMapper;
import com.rail.api.dto.TaskDto;
import com.rail.api.entity.Task;
import com.rail.api.entity.TaskCompletionType;
import com.rail.api.entity.TaskStatus;
import com.rail.api.entity.User;
import com.rail.api.repository.GoalRepository;
import com.rail.api.repository.TaskRepository;
import com.rail.api.repository.TaskTargetRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskTargetRepository taskTargetRepository;
    private final GoalRepository goalRepository;
    private final TaskCompletionService completionService;
    private final DtoMapper dtoMapper;

    @Transactional
    public TaskDto complete(
        UUID taskPid,
        User user,
        TaskCompletionType completionType,
        String completionNote,
        BigDecimal actualValue
    ) {
        Task task = resolveOwned(taskPid, user);

        task.setStatus(TaskStatus.DONE);
        task.setCompletionType(completionType != null ? completionType : TaskCompletionType.FULL);
        task.setCompletionNote(completionNote);
        task.setCompletedAt(Instant.now());
        task.setEndedAt(Instant.now());

        long addedMinutes = task.getDurationMinutes() != null ? task.getDurationMinutes() : 0;
        var goal = task.getGoal();
        goal.setActualTotalHours(goal.getActualTotalHours() + (addedMinutes / 60));
        goalRepository.save(goal);

        taskTargetRepository.findByTask(task).ifPresent(tt -> {
            tt.setActualValue(
                actualValue != null ? actualValue : tt.getEstimatedValue()
            );
            taskTargetRepository.save(tt);
        });

        taskRepository.save(task);

        if (task.getMilestone() != null) {
            completionService.recalcMilestone(task.getMilestone());
        }

        completionService.recalcGoalCompletion(goal);

        return dtoMapper.toTaskDto(task);
    }

    @Transactional
    public TaskDto skip(UUID taskPid, User user, String reason) {
        Task task = resolveOwned(taskPid, user);

        task.setStatus(TaskStatus.SKIPPED);
        task.setMissReason(reason);
        taskRepository.save(task);

        return dtoMapper.toTaskDto(task);
    }

    private Task resolveOwned(UUID taskPid, User user) {
        Task task = taskRepository.findByPid(taskPid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        if (!task.getGoal().getIntention().getOwner().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return task;
    }
}
