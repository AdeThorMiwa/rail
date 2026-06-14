import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../data/models/schedule_models.dart';
import '../providers/home_provider.dart';
import '../../intentions/providers/goals_provider.dart';
import 'complete_task_sheet.dart';
import 'skip_task_sheet.dart';
import 'slip_task_sheet.dart';

Future<void> showEntryCompleteSheet(
  BuildContext context,
  WidgetRef ref,
  ScheduleEntry entry,
) async {
  final task = entry.task!;
  final repo = ref.read(homeRepositoryProvider);
  final result = await showModalBottomSheet<bool>(
    context: context,
    isScrollControlled: true,
    backgroundColor: Colors.white,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
    ),
    builder: (_) => CompleteTaskSheet(
      taskTitle: task.title,
      hasTaskTarget: task.hasTaskTarget,
      estimatedValue: task.estimatedValue,
      targetUnit: task.targetUnit,
      onSubmit: ({required completionType, completionNote, actualValue}) =>
          repo.completeEntry(
        entry.pid,
        completionType: completionType,
        completionNote: completionNote,
        actualValue: actualValue,
      ),
    ),
  );
  if (result == true && context.mounted) {
    ref.invalidate(todayScheduleProvider);
    ref.invalidate(goalsProvider);
  }
}

Future<void> showEntrySkipSheet(
  BuildContext context,
  WidgetRef ref,
  ScheduleEntry entry,
) async {
  final task = entry.task!;
  final repo = ref.read(homeRepositoryProvider);
  final result = await showModalBottomSheet<bool>(
    context: context,
    isScrollControlled: true,
    backgroundColor: Colors.white,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
    ),
    builder: (_) => SkipTaskSheet(
      taskTitle: task.title,
      isFixed: task.isFixed,
      onSubmit: (reason) => repo.skipEntry(entry.pid, reason: reason),
    ),
  );
  if (result == true && context.mounted) {
    ref.invalidate(todayScheduleProvider);
  }
}

Future<void> showEntrySlipSheet(
  BuildContext context,
  WidgetRef ref,
  ScheduleEntry entry,
) async {
  final task = entry.task!;
  final repo = ref.read(homeRepositoryProvider);
  final result = await showModalBottomSheet<bool>(
    context: context,
    isScrollControlled: true,
    backgroundColor: Colors.white,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
    ),
    builder: (_) => SlipTaskSheet(
      taskTitle: task.title,
      onSubmit: (note) => repo.slipEntry(entry.pid, note: note),
    ),
  );
  if (result == true && context.mounted) {
    ref.invalidate(todayScheduleProvider);
  }
}
