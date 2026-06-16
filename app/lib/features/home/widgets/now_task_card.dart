import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import '../data/models/schedule_models.dart';
import 'pulse_dot.dart';
import 'task_action_sheets.dart';

class NowTaskCard extends ConsumerWidget {
  final ScheduleEntry entry;

  const NowTaskCard({super.key, required this.entry});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final task = entry.task!;
    final gradient = _gradientFor(task.goalType);
    final shadow = _shadowFor(task.goalType);
    final isAbstinence = task.goalType == 'ABSTINENCE';

    return Dismissible(
      key: ValueKey('now-${entry.pid}'),
      direction: DismissDirection.horizontal,
      confirmDismiss: (direction) async {
        if (direction == DismissDirection.startToEnd) {
          await showEntryCompleteSheet(context, ref, entry);
        } else if (isAbstinence) {
          await showEntrySlipSheet(context, ref, entry);
        } else {
          await showEntrySkipSheet(context, ref, entry);
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
        color: isAbstinence ? const Color(0xFF2EA69F) : const Color(0xFFFF6B6B),
        label: isAbstinence ? 'Slip' : 'Skip',
        trailingIcon: Icons.chevron_right_rounded,
      ),
      child: GestureDetector(
        onTap: () => context.push(
          '/chat/TASK/${task.pid}?title=${Uri.encodeComponent(task.title)}',
        ),
        child: Container(
          margin: const EdgeInsets.fromLTRB(16, 0, 16, 0),
          decoration: BoxDecoration(
            gradient: gradient,
            borderRadius: BorderRadius.circular(16),
            boxShadow: [
              BoxShadow(
                color: shadow,
                blurRadius: 40,
                offset: const Offset(0, 12),
              ),
            ],
          ),
          clipBehavior: Clip.antiAlias,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(20, 20, 20, 16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        if (task.goalTitle != null) ...[
                          _GoalBadge(label: task.goalTitle!),
                          const SizedBox(width: 6),
                        ],
                        _NowBadge(),
                        const Spacer(),
                        Text(
                          '${_fmt(entry.startTime)} · ${entry.durationMinutes}m',
                          style: GoogleFonts.nunito(
                            fontSize: 11,
                            fontWeight: FontWeight.w700,
                            color: Colors.white.withValues(alpha: 0.65),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 14),
                    Text(
                      task.title,
                      style: GoogleFonts.nunito(
                        fontSize: 21,
                        fontWeight: FontWeight.w900,
                        color: Colors.white,
                        height: 1.3,
                      ),
                    ),
                    if (task.notes != null && task.notes!.isNotEmpty) ...[
                      const SizedBox(height: 8),
                      Text(
                        task.notes!,
                        style: GoogleFonts.nunito(
                          fontSize: 13,
                          fontWeight: FontWeight.w600,
                          color: Colors.white.withValues(alpha: 0.72),
                          height: 1.5,
                        ),
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ],
                  ],
                ),
              ),
              Container(
                decoration: BoxDecoration(
                  border: Border(
                    top: BorderSide(
                      color: Colors.white.withValues(alpha: 0.15),
                    ),
                  ),
                ),
                child: Row(
                  children: [
                    Expanded(
                      child: GestureDetector(
                        behavior: HitTestBehavior.opaque,
                        onTap: () => isAbstinence
                            ? showEntrySlipSheet(context, ref, entry)
                            : showEntrySkipSheet(context, ref, entry),
                        child: Padding(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 20,
                            vertical: 14,
                          ),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Icon(
                                Icons.chevron_left_rounded,
                                size: 12,
                                color: Colors.white.withValues(alpha: 0.65),
                              ),
                              const SizedBox(width: 4),
                              Text(
                                isAbstinence ? 'Log slip' : 'Skip',
                                style: GoogleFonts.nunito(
                                  fontSize: 11,
                                  fontWeight: FontWeight.w800,
                                  color: Colors.white.withValues(alpha: 0.65),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),
                    Container(
                      width: 1,
                      height: 14,
                      color: Colors.white.withValues(alpha: 0.2),
                    ),
                    Expanded(
                      child: GestureDetector(
                        behavior: HitTestBehavior.opaque,
                        onTap: () =>
                            showEntryCompleteSheet(context, ref, entry),
                        child: Padding(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 20,
                            vertical: 14,
                          ),
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.end,
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Text(
                                'Mark done',
                                style: GoogleFonts.nunito(
                                  fontSize: 11,
                                  fontWeight: FontWeight.w900,
                                  color: Colors.white.withValues(alpha: 0.9),
                                ),
                              ),
                              const SizedBox(width: 4),
                              Icon(
                                Icons.chevron_right_rounded,
                                size: 12,
                                color: Colors.white.withValues(alpha: 0.9),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
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

  String _fmt(TimeOfDay t) {
    final h = t.hourOfPeriod == 0 ? 12 : t.hourOfPeriod;
    final m = t.minute.toString().padLeft(2, '0');
    final p = t.period == DayPeriod.am ? 'AM' : 'PM';
    return '$h:$m $p';
  }
}

class _GoalBadge extends StatelessWidget {
  final String label;
  const _GoalBadge({required this.label});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.22),
        borderRadius: BorderRadius.circular(6),
      ),
      width: 150,
      child: Text(
        label,
        overflow: TextOverflow.ellipsis,
        style: GoogleFonts.nunito(
          fontSize: 11,
          fontWeight: FontWeight.w800,
          color: Colors.white,
          letterSpacing: 0.3,
        ),
      ),
    );
  }
}

class _NowBadge extends StatelessWidget {
  const _NowBadge();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.22),
        borderRadius: BorderRadius.circular(6),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          const PulseDot(size: 6),
          const SizedBox(width: 5),
          Text(
            'NOW',
            style: GoogleFonts.nunito(
              fontSize: 10,
              fontWeight: FontWeight.w900,
              color: Colors.white,
              letterSpacing: 0.4,
            ),
          ),
        ],
      ),
    );
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
      margin: EdgeInsets.zero,
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(16),
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
