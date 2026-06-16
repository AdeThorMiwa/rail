package com.rail.api.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rail.api.dto.ChatMessageDto;
import com.rail.api.dto.DailyScheduleDto;
import com.rail.api.dto.GoalDto;
import com.rail.api.dto.GoalTargetDto;
import com.rail.api.dto.HabitStatsDto;
import com.rail.api.dto.IntentionDto;
import com.rail.api.dto.MilestoneDto;
import com.rail.api.dto.ScheduleEntryDto;
import com.rail.api.dto.TaskDto;
import com.rail.api.entity.ChatMessage;
import com.rail.api.entity.DailySchedule;
import com.rail.api.entity.DailyScheduleEntry;
import com.rail.api.entity.Goal;
import com.rail.api.entity.Intention;
import com.rail.api.entity.MessageSender;
import com.rail.api.entity.Milestone;
import com.rail.api.entity.Task;
import com.rail.api.repository.GoalTargetRepository;
import com.rail.api.repository.TaskTargetRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DtoMapper {

    private final ObjectMapper objectMapper;
    private final TaskTargetRepository taskTargetRepository;
    private final GoalTargetRepository goalTargetRepository;

    public ChatMessageDto toChatMessageDto(ChatMessage msg) {
        try {
            JsonNode blocks = objectMapper.readTree(msg.getBlocks());
            return new ChatMessageDto(
                msg.getPid().toString(),
                msg.getChat().getPid().toString(),
                msg.getSender() == MessageSender.USER ? "user" : "connie",
                msg.getVariant(),
                blocks,
                msg.getCreatedAt(),
                msg.getReplyTo() != null ? msg.getReplyTo().getPid().toString() : null,
                msg.getChat().getEntityType().name(),
                msg.getChat().getEntityId()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse message blocks", e);
        }
    }

    public IntentionDto toIntentionDto(
        Intention intention,
        GoalDto activeGoal
    ) {
        return new IntentionDto(
            intention.getPid(),
            intention.getRawInput(),
            intention.getTitle(),
            intention.getCompletionCriteria(),
            intention.getType(),
            intention.getStatus(),
            intention.getCreatedAt(),
            activeGoal
        );
    }

    public GoalDto toGoalDto(
        Goal goal,
        String intentionTitle,
        GoalTargetDto target,
        HabitStatsDto habitStats,
        List<Milestone> milestones,
        List<Task> todaysTasks
    ) {
        return new GoalDto(
            goal.getPid(),
            goal.getTitle(),
            goal.getType(),
            goal.getStatus(),
            goal.getEnergyLevel(),
            goal.getEstimatedTotalHours(),
            goal.getActualTotalHours(),
            goal.getTargetDate(),
            intentionTitle,
            target,
            habitStats,
            milestones.stream().map(this::toMilestoneDto).toList(),
            todaysTasks.stream().map(this::toTaskDto).toList()
        );
    }

    public MilestoneDto toMilestoneDto(Milestone m) {
        return new MilestoneDto(
            m.getPid(),
            m.getTitle(),
            m.getPosition(),
            m.getStatus(),
            m.getTargetDate()
        );
    }

    public TaskDto toTaskDto(Task t) {
        var taskTarget = taskTargetRepository.findByTask(t);
        var goalTarget = taskTarget.isPresent()
            ? goalTargetRepository.findByGoal(t.getGoal()).orElse(null)
            : null;
        return new TaskDto(
            t.getPid(),
            t.getTitle(),
            t.getNotes(),
            t.getStatus(),
            t.getPriority(),
            t.getFlexibility(),
            t.getDurationMinutes(),
            t.getMilestone() != null ? t.getMilestone().getPid() : null,
            t.getGoal().getTitle(),
            t.getGoal().getType(),
            taskTarget.isPresent(),
            taskTarget.map(tt -> tt.getEstimatedValue()).orElse(null),
            goalTarget != null ? goalTarget.getUnit() : null,
            t.getMissReason()
        );
    }

    public DailyScheduleDto toDailyScheduleDto(
        DailySchedule schedule,
        List<DailyScheduleEntry> entries
    ) {
        return new DailyScheduleDto(
            schedule.getPid(),
            schedule.getScheduledDate(),
            schedule.getStatus(),
            schedule.getRailNotes(),
            entries.stream().map(this::toEntryDto).toList()
        );
    }

    public ScheduleEntryDto toEntryDto(DailyScheduleEntry e) {
        return new ScheduleEntryDto(
            e.getPid(),
            e.getEntryType(),
            e.getStatus(),
            e.getStartTime(),
            e.getEndTime(),
            e.getNotes(),
            e.getSkipReason(),
            e.getTask() != null ? toTaskDto(e.getTask()) : null
        );
    }
}
