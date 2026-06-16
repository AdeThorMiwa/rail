import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import '../data/models/schedule_models.dart';
import '../providers/home_provider.dart';
import '../../intentions/providers/goals_provider.dart';
import 'complete_task_sheet.dart';
import 'skip_task_sheet.dart';
import 'slip_task_sheet.dart';

class TaskEntryCard extends ConsumerStatefulWidget {
  final ScheduleEntry entry;

  const TaskEntryCard({super.key, required this.entry});

  @override
  ConsumerState<TaskEntryCard> createState() => _TaskEntryCardState();
}

class _TaskEntryCardState extends ConsumerState<TaskEntryCard> {
  void _openTaskChat() {
    final task = widget.entry.task!;
    context.push(
      '/chat/TASK/${task.pid}?title=${Uri.encodeComponent(task.title)}',
    );
  }

  Future<void> _showCompleteSheet() async {
    final entry = widget.entry;
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
    if (result == true && mounted) {
      ref.invalidate(todayScheduleProvider);
      ref.invalidate(goalsProvider);
    }
  }

  Future<void> _showSkipSheet() async {
    final entry = widget.entry;
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
    if (result == true && mounted) {
      ref.invalidate(todayScheduleProvider);
    }
  }

  Future<void> _showSlipSheet() async {
    final entry = widget.entry;
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
    if (result == true && mounted) {
      ref.invalidate(todayScheduleProvider);
    }
  }

  @override
  Widget build(BuildContext context) {
    final task = widget.entry.task!;
    final isInactive = !widget.entry.isPending;
    final gradient = _gradientFor(task.goalType);
    final shadow = _shadowFor(task.goalType);
    final isNow = _isNow(
      widget.entry.startTime,
      widget.entry.endTime,
      TimeOfDay.now(),
    );

    final card = AnimatedOpacity(
      duration: const Duration(milliseconds: 300),
      opacity: isInactive ? 0.38 : 1.0,
      child: Container(
        margin: const EdgeInsets.only(bottom: 12),
        decoration: BoxDecoration(
          gradient: gradient,
          borderRadius: BorderRadius.circular(8),
          boxShadow: isInactive
              ? []
              : [
                  BoxShadow(
                    color: shadow,
                    blurRadius: 28,
                    offset: const Offset(0, 10),
                  ),
                ],
        ),
        child: Material(
          color: Colors.transparent,
          child: InkWell(
            onTap: isInactive ? null : _openTaskChat,
            borderRadius: BorderRadius.circular(8),
            splashColor: Colors.white.withValues(alpha: 0.08),
            highlightColor: Colors.white.withValues(alpha: 0.04),
            child: Padding(
              padding: const EdgeInsets.all(18),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      if (task.goalTitle != null)
                        Flexible(
                          child: Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 10,
                              vertical: 3,
                            ),
                            decoration: BoxDecoration(
                              color: Colors.white.withValues(alpha: 0.22),
                              borderRadius: BorderRadius.circular(6),
                            ),
                            child: Text(
                              task.goalTitle!,
                              overflow: TextOverflow.ellipsis,
                              style: GoogleFonts.nunito(
                                fontSize: 11,
                                fontWeight: FontWeight.w800,
                                color: Colors.white,
                                letterSpacing: 0.3,
                              ),
                            ),
                          ),
                        ),
                      if (isNow && !isInactive) ...[
                        const SizedBox(width: 6),
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 8,
                            vertical: 2,
                          ),
                          decoration: BoxDecoration(
                            color: Colors.white.withValues(alpha: 0.28),
                            borderRadius: BorderRadius.circular(6),
                          ),
                          child: Text(
                            'NOW',
                            style: GoogleFonts.nunito(
                              fontSize: 10,
                              fontWeight: FontWeight.w900,
                              color: Colors.white,
                              letterSpacing: 0.4,
                            ),
                          ),
                        ),
                      ],
                      const Spacer(),
                      Text(
                        _formatTime(widget.entry.startTime),
                        style: GoogleFonts.nunito(
                          fontSize: 11,
                          fontWeight: FontWeight.w700,
                          color: Colors.white.withValues(alpha: 0.65),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Text(
                        '${widget.entry.durationMinutes}m',
                        style: GoogleFonts.nunito(
                          fontSize: 11,
                          fontWeight: FontWeight.w700,
                          color: Colors.white.withValues(alpha: 0.65),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 10),
                  Text(
                    task.title,
                    style: GoogleFonts.nunito(
                      fontSize: 18,
                      fontWeight: FontWeight.w900,
                      color: Colors.white,
                      height: 1.3,
                      decoration: isInactive
                          ? TextDecoration.lineThrough
                          : null,
                      decorationColor: Colors.white,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );

    if (isInactive) return card;

    return Dismissible(
      key: ValueKey('entry-${widget.entry.pid}'),
      direction: DismissDirection.horizontal,
      confirmDismiss: (direction) async {
        if (direction == DismissDirection.startToEnd) {
          await _showCompleteSheet();
        } else if (task.goalType == 'ABSTINENCE') {
          await _showSlipSheet();
        } else {
          await _showSkipSheet();
        }
        return false;
      },
      background: _SwipeBg(
        alignment: Alignment.centerLeft,
        color: const Color(0xFF22C55E),
        label: 'Done',
        leadingIcon: Icons.check_rounded,
      ),
      secondaryBackground: _SwipeBg(
        alignment: Alignment.centerRight,
        color: task.goalType == 'ABSTINENCE'
            ? const Color(0xFF2EA69F)
            : const Color(0xFFFF6B6B),
        label: task.goalType == 'ABSTINENCE' ? 'Slip' : 'Skip',
        trailingIcon: Icons.chevron_right_rounded,
      ),
      child: card,
    );
  }

  LinearGradient _gradientFor(String? goalType) => switch (goalType) {
    'HABIT' => const LinearGradient(
      begin: Alignment.topLeft,
      end: Alignment.bottomRight,
      colors: [Color(0xFF3A8FEF), Color(0xFF7BB3FF)],
    ),
    'ABSTINENCE' => const LinearGradient(
      begin: Alignment.topLeft,
      end: Alignment.bottomRight,
      colors: [Color(0xFF2EA69F), Color(0xFF5BBFB8)],
    ),
    'TASK' => const LinearGradient(
      begin: Alignment.topLeft,
      end: Alignment.bottomRight,
      colors: [Color(0xFF9061E0), Color(0xFFBC98FF)],
    ),
    'QUANTIFIED' => const LinearGradient(
      begin: Alignment.topLeft,
      end: Alignment.bottomRight,
      colors: [Color(0xFF0E9FC8), Color(0xFF38BEF5)],
    ),
    _ => const LinearGradient(
      begin: Alignment.topLeft,
      end: Alignment.bottomRight,
      colors: [Color(0xFF6355EE), Color(0xFF9B8FFF)],
    ),
  };

  Color _shadowFor(String? goalType) => switch (goalType) {
    'HABIT' => const Color(0xFF3A8FEF).withValues(alpha: 0.38),
    'ABSTINENCE' => const Color(0xFF2EA69F).withValues(alpha: 0.35),
    'TASK' => const Color(0xFF9061E0).withValues(alpha: 0.38),
    'QUANTIFIED' => const Color(0xFF0E9FC8).withValues(alpha: 0.35),
    _ => const Color(0xFF6355EE).withValues(alpha: 0.42),
  };

  bool _isNow(TimeOfDay start, TimeOfDay end, TimeOfDay now) {
    final startMins = start.hour * 60 + start.minute;
    final endMins = end.hour * 60 + end.minute;
    final nowMins = now.hour * 60 + now.minute;
    return nowMins >= startMins && nowMins < endMins;
  }

  String _formatTime(TimeOfDay t) {
    final hour = t.hourOfPeriod == 0 ? 12 : t.hourOfPeriod;
    final min = t.minute.toString().padLeft(2, '0');
    final period = t.period == DayPeriod.am ? 'AM' : 'PM';
    return '$hour:$min $period';
  }
}

class _SwipeBg extends StatelessWidget {
  final Alignment alignment;
  final Color color;
  final String label;
  final IconData? leadingIcon;
  final IconData? trailingIcon;

  const _SwipeBg({
    required this.alignment,
    required this.color,
    required this.label,
    this.leadingIcon,
    this.trailingIcon,
  });

  @override
  Widget build(BuildContext context) {
    final textWidget = Text(
      label,
      style: GoogleFonts.nunito(
        fontSize: 13,
        fontWeight: FontWeight.w900,
        color: Colors.white,
      ),
    );
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(8),
      ),
      alignment: alignment,
      padding: const EdgeInsets.symmetric(horizontal: 20),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (leadingIcon != null) ...[
            Icon(leadingIcon, size: 15, color: Colors.white),
            const SizedBox(width: 6),
          ],
          textWidget,
          if (trailingIcon != null) ...[
            const SizedBox(width: 6),
            Icon(trailingIcon, size: 15, color: Colors.white),
          ],
        ],
      ),
    );
  }
}
