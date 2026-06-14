import 'package:flutter/material.dart';

enum ScheduleEntryType { task, free, buffer }

enum ScheduleEntryTimeState { upcoming, active, overdue }

enum DailyScheduleStatus { generating, planned, inProgress, completed }

enum TaskStatus { pending, done, skipped, missed }

enum ScheduleEntryStatus { pending, completed, skipped, missed, removed }

class TaskSummary {
  final String pid;
  final String title;
  final String? notes;
  final String? goalTitle;
  final String? goalType;
  final TaskStatus status;
  final bool hasTaskTarget;
  final double? estimatedValue;
  final String? targetUnit;
  final String? flexibility;
  final String? missReason;

  const TaskSummary({
    required this.pid,
    required this.title,
    this.notes,
    this.goalTitle,
    this.goalType,
    this.status = TaskStatus.pending,
    this.hasTaskTarget = false,
    this.estimatedValue,
    this.targetUnit,
    this.flexibility,
    this.missReason,
  });

  bool get isFixed => flexibility == 'FIXED';

  factory TaskSummary.fromJson(Map<String, dynamic> j) => TaskSummary(
        pid: j['pid'] as String,
        title: j['title'] as String,
        notes: j['notes'] as String?,
        goalTitle: j['goalTitle'] as String?,
        goalType: j['goalType'] as String?,
        status: _parseStatus(j['status'] as String? ?? 'PENDING'),
        hasTaskTarget: j['hasTaskTarget'] as bool? ?? false,
        estimatedValue: (j['estimatedValue'] as num?)?.toDouble(),
        targetUnit: j['targetUnit'] as String?,
        flexibility: j['flexibility'] as String?,
        missReason: j['missReason'] as String?,
      );

  static TaskStatus _parseStatus(String s) => switch (s) {
        'DONE'    => TaskStatus.done,
        'SKIPPED' => TaskStatus.skipped,
        'MISSED'  => TaskStatus.missed,
        _         => TaskStatus.pending,
      };

  bool get isDone => status == TaskStatus.done;
  bool get isActive => status == TaskStatus.pending;
}

class ScheduleEntry {
  final String pid;
  final ScheduleEntryType entryType;
  final ScheduleEntryStatus entryStatus;
  final TimeOfDay startTime;
  final TimeOfDay endTime;
  final String? notes;
  final String? skipReason;
  final TaskSummary? task;

  const ScheduleEntry({
    required this.pid,
    required this.entryType,
    this.entryStatus = ScheduleEntryStatus.pending,
    required this.startTime,
    required this.endTime,
    this.notes,
    this.skipReason,
    this.task,
  });

  factory ScheduleEntry.fromJson(Map<String, dynamic> j) {
    return ScheduleEntry(
      pid: j['pid'] as String,
      entryType: _parseEntryType(j['entryType'] as String),
      entryStatus: _parseEntryStatus(j['entryStatus'] as String? ?? 'PENDING'),
      startTime: _parseTime(j['startTime'] as String),
      endTime: _parseTime(j['endTime'] as String),
      notes: j['notes'] as String?,
      skipReason: j['skipReason'] as String?,
      task: j['task'] != null
          ? TaskSummary.fromJson(j['task'] as Map<String, dynamic>)
          : null,
    );
  }

  static ScheduleEntryStatus _parseEntryStatus(String s) => switch (s) {
        'COMPLETED' => ScheduleEntryStatus.completed,
        'SKIPPED'   => ScheduleEntryStatus.skipped,
        'MISSED'    => ScheduleEntryStatus.missed,
        'REMOVED'   => ScheduleEntryStatus.removed,
        _           => ScheduleEntryStatus.pending,
      };

  bool get isPending => entryStatus == ScheduleEntryStatus.pending;
  bool get isCompleted => entryStatus == ScheduleEntryStatus.completed;
  bool get isSkipped => entryStatus == ScheduleEntryStatus.skipped;
  bool get isMissed => entryStatus == ScheduleEntryStatus.missed;

  ScheduleEntryTimeState timeStateAt(TimeOfDay now) {
    final nowMins = now.hour * 60 + now.minute;
    final startMins = startTime.hour * 60 + startTime.minute;
    final endMins = endTime.hour * 60 + endTime.minute;
    if (nowMins < startMins) return ScheduleEntryTimeState.upcoming;
    if (nowMins < endMins) return ScheduleEntryTimeState.active;
    return ScheduleEntryTimeState.overdue;
  }

  int get durationMinutes {
    final startMins = startTime.hour * 60 + startTime.minute;
    final endMins = endTime.hour * 60 + endTime.minute;
    return endMins - startMins;
  }

  static ScheduleEntryType _parseEntryType(String s) => switch (s) {
        'FREE' => ScheduleEntryType.free,
        'BUFFER' => ScheduleEntryType.buffer,
        _ => ScheduleEntryType.task,
      };

  static TimeOfDay _parseTime(String s) {
    final parts = s.split(':');
    return TimeOfDay(hour: int.parse(parts[0]), minute: int.parse(parts[1]));
  }
}

class DailySchedule {
  final String pid;
  final DateTime scheduledDate;
  final DailyScheduleStatus status;
  final String? railNotes;
  final List<ScheduleEntry> entries;

  const DailySchedule({
    required this.pid,
    required this.scheduledDate,
    required this.status,
    this.railNotes,
    required this.entries,
  });

  factory DailySchedule.fromJson(Map<String, dynamic> j) => DailySchedule(
        pid: j['pid'] as String,
        scheduledDate: DateTime.parse(j['scheduledDate'] as String),
        status: _parseStatus(j['status'] as String),
        railNotes: j['railNotes'] as String?,
        entries: (j['entries'] as List)
            .map((e) => ScheduleEntry.fromJson(e as Map<String, dynamic>))
            .toList(),
      );

  List<ScheduleEntry> get taskEntries =>
      entries.where((e) => e.entryType == ScheduleEntryType.task).toList();

  static DailyScheduleStatus _parseStatus(String s) => switch (s) {
        'GENERATING' => DailyScheduleStatus.generating,
        'IN_PROGRESS' => DailyScheduleStatus.inProgress,
        'COMPLETED' => DailyScheduleStatus.completed,
        _ => DailyScheduleStatus.planned,
      };
}
