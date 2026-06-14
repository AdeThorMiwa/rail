import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import '../data/models/schedule_models.dart';
import 'task_action_sheets.dart';

class OverdueTaskCard extends ConsumerWidget {
  final ScheduleEntry entry;

  const OverdueTaskCard({super.key, required this.entry});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final task = entry.task!;
    final isAbstinence = task.goalType == 'ABSTINENCE';

    final card = Container(
      margin: const EdgeInsets.fromLTRB(16, 0, 16, 10),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: const Color(0xFFFFD6D6), width: 1.5),
      ),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Row(
          children: [
            SizedBox(
              width: 46,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    _fmt(entry.startTime),
                    style: GoogleFonts.nunito(
                      fontSize: 12,
                      fontWeight: FontWeight.w800,
                      color: const Color(0xFFEF5350),
                    ),
                  ),
                  Text(
                    '${entry.durationMinutes}m',
                    style: GoogleFonts.nunito(
                      fontSize: 10,
                      fontWeight: FontWeight.w700,
                      color: const Color(0xFFFFAAAA),
                    ),
                  ),
                ],
              ),
            ),
            Container(
              width: 1,
              height: 38,
              color: const Color(0xFFFFE0E0),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (task.goalTitle != null)
                    Text(
                      '${(task.goalType ?? 'TASK').toUpperCase()}  ·  ${task.goalTitle}',
                      style: GoogleFonts.nunito(
                        fontSize: 10,
                        fontWeight: FontWeight.w800,
                        color: const Color(0xFFEF5350).withValues(alpha: 0.7),
                        letterSpacing: 0.2,
                      ),
                      overflow: TextOverflow.ellipsis,
                    ),
                  Text(
                    task.title,
                    style: GoogleFonts.nunito(
                      fontSize: 14,
                      fontWeight: FontWeight.w800,
                      color: const Color(0xFF1A1A2E),
                    ),
                    overflow: TextOverflow.ellipsis,
                  ),
                  const SizedBox(height: 3),
                  Row(
                    children: [
                      const Text('⏰', style: TextStyle(fontSize: 10)),
                      const SizedBox(width: 3),
                      Text(
                        'Window closed · ended ${_fmt(entry.endTime)}',
                        style: GoogleFonts.nunito(
                          fontSize: 10,
                          fontWeight: FontWeight.w800,
                          color: const Color(0xFFEF5350),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(width: 8),
            Text(
              '↔ Swipe\nto log',
              textAlign: TextAlign.right,
              style: GoogleFonts.nunito(
                fontSize: 9,
                fontWeight: FontWeight.w700,
                color: const Color(0xFFFFAAAA),
                height: 1.4,
              ),
            ),
          ],
        ),
      ),
    );

    return Dismissible(
      key: ValueKey('overdue-${entry.pid}'),
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
      child: card,
    );
  }

  String _fmt(TimeOfDay t) {
    final h = t.hourOfPeriod == 0 ? 12 : t.hourOfPeriod;
    final m = t.minute.toString().padLeft(2, '0');
    final p = t.period == DayPeriod.am ? 'AM' : 'PM';
    return '$h:$m $p';
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
      margin: const EdgeInsets.fromLTRB(16, 0, 16, 10),
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(12),
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
