package com.rail.api.controller;

import com.rail.api.component.UserResolver;
import com.rail.api.dto.TaskDto;
import com.rail.api.entity.TaskCompletionType;
import com.rail.api.service.TaskService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final UserResolver userResolver;

    @GetMapping
    public List<TaskDto> list(
        @AuthenticationPrincipal UUID userPid,
        @RequestParam(required = false) UUID goalId,
        @RequestParam(required = false) UUID milestoneId
    ) {
        return taskService.list(userResolver.resolve(userPid), goalId, milestoneId);
    }

    public record CompleteTaskRequest(
        TaskCompletionType completionType,
        String completionNote,
        BigDecimal actualValue
    ) {}

    public record SkipTaskRequest(String reason) {}

    @PostMapping("/{pid}/complete")
    public ResponseEntity<TaskDto> complete(
        @PathVariable UUID pid,
        @AuthenticationPrincipal UUID userPid,
        @RequestBody CompleteTaskRequest body
    ) {
        TaskDto dto = taskService.complete(
            pid,
            userResolver.resolve(userPid),
            body.completionType(),
            body.completionNote(),
            body.actualValue()
        );
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{pid}/skip")
    public ResponseEntity<TaskDto> skip(
        @PathVariable UUID pid,
        @AuthenticationPrincipal UUID userPid,
        @RequestBody SkipTaskRequest body
    ) {
        TaskDto dto = taskService.skip(pid, userResolver.resolve(userPid), body.reason());
        return ResponseEntity.ok(dto);
    }
}
