package com.rail.api.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rail.api.entity.EnergyLevel;
import com.rail.api.entity.Task;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
class SchedulingLlmClient {

    private static final DateTimeFormatter FILE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    @Value("${rail.scheduling.model:deepseek-v4-pro}")
    private String schedulingModel;

    @Value("${rail.llm.max-json-retries:2}")
    private int maxJsonRetries;

    @Value("${rail.llm.log-dir}")
    private String logDir;

    @Value("${rail.llm.log-enabled}")
    private boolean llmLogEnabled;

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    LlmResult callWithRetry(
        String systemPrompt,
        List<Task> flexibleTasks,
        LocalTime deepStart,
        LocalTime deepEnd,
        LocalTime effectiveStart,
        LocalTime dayEnd,
        LocalDate date,
        int bufferMinutes
    ) {
        List<Message> messages = List.of(
            new SystemMessage(systemPrompt),
            new UserMessage("Build today's schedule.")
        );

        log.info("[scheduler] calling LLM — model={} tasks={}", schedulingModel, flexibleTasks.size());
        String raw = invokeModel(messages, 0);
        log.info("[scheduler] LLM responded (attempt 0) — response length={}", raw == null ? 0 : raw.length());
        writeLlmLog(messages, raw, 0);
        SchedulingPlan plan = parsePlan(raw);

        for (int attempt = 1; attempt <= maxJsonRetries && plan == null; attempt++) {
            List<Message> retryMessages = new ArrayList<>(messages);
            retryMessages.add(new AssistantMessage(raw));
            retryMessages.add(new UserMessage(
                "Your response was not valid JSON matching the required schema. " +
                "Return ONLY the JSON object. No explanation, no markdown fences."
            ));
            log.warn("[scheduler] LLM parse failed — retrying (attempt {})", attempt);
            raw = invokeModel(retryMessages, attempt);
            log.info("[scheduler] LLM responded (attempt {}) — response length={}", attempt, raw == null ? 0 : raw.length());
            writeLlmLog(retryMessages, raw, attempt);
            plan = parsePlan(raw);
        }

        if (plan == null) {
            log.error("Scheduling LLM failed after {} retries — using linear fallback", maxJsonRetries);
            return new LlmResult(linearFallback(flexibleTasks, deepStart, deepEnd, effectiveStart, dayEnd, date, bufferMinutes), true);
        }

        return new LlmResult(plan, false);
    }

    SchedulingPlan linearFallback(
        List<Task> tasks,
        LocalTime deepStart,
        LocalTime deepEnd,
        LocalTime effectiveStart,
        LocalTime dayEnd,
        LocalDate date,
        int bufferMinutes
    ) {
        int deepMinutes = (int) Duration.between(deepStart, deepEnd).toMinutes();
        int outerMinutes = (int) Duration.between(effectiveStart, deepStart).toMinutes()
            + (int) Duration.between(deepEnd, dayEnd).toMinutes();
        int deepUsed = 0, outerUsed = 0;
        List<SchedulingPlan.SelectedTask> selected = new ArrayList<>();
        int order = 1;

        for (Task task : tasks) {
            int dur = task.getDurationMinutes() != null ? task.getDurationMinutes() : 60;
            EnergyLevel energy = task.getGoal().getEnergyLevel();
            boolean deadline = task.getDeadline() != null && task.getDeadline().equals(date);

            if (energy == EnergyLevel.DEEP) {
                if (deepUsed + dur + bufferMinutes <= deepMinutes) {
                    selected.add(new SchedulingPlan.SelectedTask(task.getPid().toString(), order++, "DEEP_WINDOW", deadline, "Linear fallback"));
                    deepUsed += dur + bufferMinutes;
                }
            } else if (energy == EnergyLevel.LIGHT) {
                if (deepUsed + dur + bufferMinutes <= deepMinutes) {
                    selected.add(new SchedulingPlan.SelectedTask(task.getPid().toString(), order++, "DEEP_WINDOW", deadline, "Linear fallback"));
                    deepUsed += dur + bufferMinutes;
                } else if (outerUsed + dur + bufferMinutes <= outerMinutes) {
                    selected.add(new SchedulingPlan.SelectedTask(task.getPid().toString(), order++, "OUTER", deadline, "Linear fallback"));
                    outerUsed += dur + bufferMinutes;
                }
            } else {
                // ADMIN
                if (outerUsed + dur + bufferMinutes <= outerMinutes) {
                    selected.add(new SchedulingPlan.SelectedTask(task.getPid().toString(), order++, "OUTER", deadline, "Linear fallback"));
                    outerUsed += dur + bufferMinutes;
                }
            }
        }

        return new SchedulingPlan(selected, List.of(), "Here's your plan for today.");
    }

    private String invokeModel(List<Message> messages, int attempt) {
        var options = OpenAiChatOptions.builder()
            .model(schedulingModel)
            .responseFormat(new ResponseFormat(ResponseFormat.Type.JSON_OBJECT, (String) null))
            .reasoningEffort("high")
            .build();

        var accumulated = new StringBuilder();
        var charCount = new AtomicInteger(0);
        Path logPath = resolveStreamLogPath(attempt);

        try (BufferedWriter writer = tryOpenStreamLog(logPath, messages, attempt)) {
            ChatClient.create(chatModel)
                .prompt()
                .messages(messages)
                .options(options)
                .stream()
                .content()
                .doOnNext(chunk -> {
                    accumulated.append(chunk);
                    int total = charCount.addAndGet(chunk.length());
                    appendToStreamLog(writer, chunk);
                    if (total % 500 < chunk.length()) {
                        log.info("[scheduler] streaming... {} chars received", total);
                    }
                })
                .doOnError(e -> {
                    log.error("[scheduler] stream error after {} chars: {}", charCount.get(), e.getMessage(), e);
                    appendToStreamLog(writer, "\n\n=== STREAM ERROR: %s ===\n".formatted(e.getMessage()));
                })
                .doOnComplete(() -> {
                    log.info("[scheduler] stream complete — {} chars total", charCount.get());
                    appendToStreamLog(writer, "\n\n=== STREAM COMPLETE: %d chars ===\n".formatted(charCount.get()));
                })
                .blockLast();
        } catch (Exception e) {
            log.error("[scheduler] invokeModel failed after {} chars: {}", charCount.get(), e.getMessage(), e);
            return null;
        }

        return accumulated.isEmpty() ? null : accumulated.toString();
    }

    private SchedulingPlan parsePlan(String raw) {
        if (raw == null) return null;
        String trimmed = raw.strip();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\n?", "");
            trimmed = trimmed.replaceAll("\\n?```$", "").strip();
        }
        try {
            return objectMapper.readValue(trimmed, SchedulingPlan.class);
        } catch (Exception e) {
            log.warn("Failed to parse scheduling plan: {}", e.getMessage());
            return null;
        }
    }

    private Path resolveStreamLogPath(int attempt) {
        String suffix = attempt > 0 ? "_retry" + attempt : "";
        return Paths.get(logDir).resolve(LocalDateTime.now().format(FILE_FMT) + "_LlmDailyScheduler_stream" + suffix + ".log");
    }

    private BufferedWriter tryOpenStreamLog(Path path, List<Message> messages, int attempt) {
        if (!llmLogEnabled) return null;
        try {
            Files.createDirectories(path.getParent());
            var writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            writer.write("scheduler: LlmDailyScheduler\nmodel    : %s\nattempt  : %d\ntime     : %s\n\n"
                .formatted(schedulingModel, attempt, LocalDateTime.now()));
            for (Message msg : messages) {
                writer.write("=== %s ===\n%s\n\n".formatted(role(msg).toUpperCase(), msg.getText()));
            }
            writer.write("=== STREAMING RESPONSE ===\n");
            writer.flush();
            return writer;
        } catch (IOException e) {
            log.warn("[scheduler] could not open stream log at {}: {}", path, e.getMessage());
            return null;
        }
    }

    private void appendToStreamLog(BufferedWriter writer, String text) {
        if (writer == null) return;
        try {
            writer.write(text);
            writer.flush();
        } catch (IOException e) {
            log.warn("[scheduler] failed to write stream log chunk: {}", e.getMessage());
        }
    }

    private void writeLlmLog(List<Message> messages, String rawOutput, int attempt) {
        if (!llmLogEnabled) return;
        try {
            Path dir = Paths.get(logDir);
            Files.createDirectories(dir);
            String suffix = attempt > 0 ? "_retry" + attempt : "";
            String filename = LocalDateTime.now().format(FILE_FMT) + "_LlmDailyScheduler" + suffix + ".log";
            String messagesDump = messages.stream()
                .map(m -> "=== %s ===\n%s\n\n".formatted(role(m).toUpperCase(), m.getText()))
                .collect(Collectors.joining());
            String content = "scheduler: LlmDailyScheduler\nmodel    : %s\nattempt  : %d\ntime     : %s\n\n%s=== RESPONSE ===\n%s\n"
                .formatted(schedulingModel, attempt, LocalDateTime.now(), messagesDump, rawOutput);
            Files.writeString(dir.resolve(filename), content);
        } catch (IOException e) {
            log.warn("Failed to write scheduler LLM log: {}", e.getMessage());
        }
    }

    private static String role(Message msg) {
        return switch (msg) {
            case SystemMessage m -> "system";
            case AssistantMessage m -> "assistant";
            default -> "user";
        };
    }
}
