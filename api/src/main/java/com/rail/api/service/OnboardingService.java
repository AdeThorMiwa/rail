package com.rail.api.service;

import com.rail.api.dto.SchedulingDayDto;
import com.rail.api.dto.SchedulingProfileDto;
import com.rail.api.dto.SchedulingProfileRequest;
import com.rail.api.entity.EnergyLevel;
import com.rail.api.entity.Goal;
import com.rail.api.entity.GoalRecurrence;
import com.rail.api.entity.GoalRecurrenceFrequency;
import com.rail.api.entity.GoalStatus;
import com.rail.api.entity.GoalType;
import com.rail.api.entity.Intention;
import com.rail.api.entity.IntentionStatus;
import com.rail.api.entity.IntentionType;
import com.rail.api.entity.Task;
import com.rail.api.entity.TaskFlexibility;
import com.rail.api.entity.TaskFlexibilitySetBy;
import com.rail.api.entity.TaskPriority;
import com.rail.api.entity.TaskStatus;
import com.rail.api.entity.User;
import com.rail.api.entity.UserSchedulingDay;
import com.rail.api.entity.UserSchedulingProfile;
import com.rail.api.repository.GoalRecurrenceRepository;
import com.rail.api.repository.GoalRepository;
import com.rail.api.repository.IntentionRepository;
import com.rail.api.repository.TaskRepository;
import com.rail.api.repository.UserSchedulingDayRepository;
import com.rail.api.repository.UserSchedulingProfileRepository;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final UserSchedulingProfileRepository profileRepository;
    private final UserSchedulingDayRepository dayRepository;
    private final IntentionRepository intentionRepository;
    private final GoalRepository goalRepository;
    private final GoalRecurrenceRepository goalRecurrenceRepository;
    private final TaskRepository taskRepository;

    @Transactional
    public SchedulingProfileDto createProfile(
        User user,
        SchedulingProfileRequest req
    ) {
        validateTimeRanges(req);
        if (profileRepository.existsByUser(user)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Scheduling profile already exists"
            );
        }
        UserSchedulingProfile profile = profileRepository.saveAndFlush(
            UserSchedulingProfile.builder()
                .user(user)
                .deepWorkStart(req.deepWorkStart())
                .deepWorkEnd(req.deepWorkEnd())
                .energyPattern(req.energyPattern())
                .wakeTime(req.wakeTime())
                .sleepTime(req.sleepTime())
                .timezone(req.timezone())
                .build()
        );
        List<UserSchedulingDay> days = saveDays(profile, req);
        seedDefaultIntentions(user, req.wakeTime(), req.sleepTime());
        return toDto(profile, days);
    }

    @Transactional(readOnly = true)
    public SchedulingProfileDto getProfile(User user) {
        UserSchedulingProfile profile = profileRepository
            .findByUser(user)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Scheduling profile not found"
                )
            );
        List<UserSchedulingDay> days =
            dayRepository.findByUserSchedulingProfile(profile);
        return toDto(profile, days);
    }

    @Transactional
    public SchedulingProfileDto updateProfile(
        User user,
        SchedulingProfileRequest req
    ) {
        validateTimeRanges(req);
        UserSchedulingProfile profile = profileRepository
            .findByUser(user)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Scheduling profile not found"
                )
            );

        profile.setDeepWorkStart(req.deepWorkStart());
        profile.setDeepWorkEnd(req.deepWorkEnd());
        profile.setEnergyPattern(req.energyPattern());
        profile.setWakeTime(req.wakeTime());
        profile.setSleepTime(req.sleepTime());
        profile.setTimezone(req.timezone());
        profileRepository.save(profile);

        dayRepository.deleteByUserSchedulingProfile(profile);
        List<UserSchedulingDay> days = saveDays(profile, req);
        return toDto(profile, days);
    }

    public boolean profileExists(User user) {
        return profileRepository.existsByUser(user);
    }

    private void validateTimeRanges(SchedulingProfileRequest req) {
        if (!req.wakeTime().isBefore(req.sleepTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wake time must be before sleep time");
        }
        if (!req.deepWorkStart().isBefore(req.deepWorkEnd())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deep work start must be before deep work end");
        }
    }

    private void seedDefaultIntentions(User user, LocalTime wakeTime, LocalTime sleepTime) {
        String wakeLabel = formatTime(wakeTime);
        String sleepLabel = formatTime(sleepTime);
        seedSleepWakeIntention(user, "Have a fixed wake time", "Wake by " + wakeLabel, wakeTime);
        seedSleepWakeIntention(user, "Have a fixed sleep time", "Sleep by " + sleepLabel, sleepTime);
    }

    private String formatTime(LocalTime t) {
        int h = t.getHour();
        int m = t.getMinute();
        String period = h < 12 ? "am" : "pm";
        int displayH = h % 12 == 0 ? 12 : h % 12;
        return m == 0 ? displayH + period : displayH + ":" + String.format("%02d", m) + period;
    }

    private void seedSleepWakeIntention(
        User user,
        String rawInput,
        String title,
        LocalTime fixedTime
    ) {
        Intention intention = intentionRepository.saveAndFlush(
            Intention.builder()
                .owner(user)
                .rawInput(rawInput)
                .title(title)
                .type(IntentionType.UNBOUNDED)
                .status(IntentionStatus.ACTIVE)
                .build()
        );
        Goal goal = goalRepository.saveAndFlush(
            Goal.builder()
                .intention(intention)
                .title(title)
                .type(GoalType.HABIT)
                .status(GoalStatus.ACTIVE)
                .energyLevel(EnergyLevel.LIGHT)
                .estimatedTotalHours(0L)
                .build()
        );
        goalRecurrenceRepository.save(
            GoalRecurrence.builder()
                .goal(goal)
                .frequency(GoalRecurrenceFrequency.DAILY)
                .timesPerPeriod(1)
                .build()
        );
        taskRepository.save(
            Task.builder()
                .goal(goal)
                .title(title)
                .status(TaskStatus.PENDING)
                .flexibility(TaskFlexibility.FIXED)
                .flexibilitySetBy(TaskFlexibilitySetBy.RAIL)
                .priority(TaskPriority.MEDIUM)
                .fixedTime(fixedTime)
                .durationMinutes(5)
                .build()
        );
    }

    private List<UserSchedulingDay> saveDays(
        UserSchedulingProfile profile,
        SchedulingProfileRequest req
    ) {
        List<UserSchedulingDay> days = req
            .days()
            .stream()
            .map(d ->
                UserSchedulingDay.builder()
                    .userSchedulingProfile(profile)
                    .dayOfWeek(d.dayOfWeek())
                    .preferredWorkStart(d.preferredWorkStart())
                    .preferredWorkEnd(d.preferredWorkEnd())
                    .build()
            )
            .toList();
        return dayRepository.saveAllAndFlush(days);
    }

    private SchedulingProfileDto toDto(
        UserSchedulingProfile profile,
        List<UserSchedulingDay> days
    ) {
        return new SchedulingProfileDto(
            profile.getPid(),
            profile.getDeepWorkStart(),
            profile.getDeepWorkEnd(),
            profile.getWakeTime(),
            profile.getSleepTime(),
            profile.getEnergyPattern(),
            profile.getTimezone(),
            days
                .stream()
                .map(d ->
                    new SchedulingDayDto(
                        d.getPid(),
                        d.getDayOfWeek(),
                        d.getPreferredWorkStart(),
                        d.getPreferredWorkEnd()
                    )
                )
                .toList()
        );
    }
}
