import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../data/models/schedule_models.dart';

class UpcomingTaskCard extends StatelessWidget {
  final ScheduleEntry entry;

  const UpcomingTaskCard({super.key, required this.entry});

  @override
  Widget build(BuildContext context) {
    final task = entry.task!;
    final countdown = _countdown();

    return Container(
      margin: const EdgeInsets.fromLTRB(16, 0, 16, 10),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: const Color(0xFFEEEAFF), width: 1.5),
      ),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Row(
          children: [
            SizedBox(
              width: 56,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    _fmt(entry.startTime),
                    style: GoogleFonts.nunito(
                      fontSize: 12,
                      fontWeight: FontWeight.w800,
                      color: const Color(0xFF6355EE),
                    ),
                  ),
                  Text(
                    '${entry.durationMinutes}m',
                    style: GoogleFonts.nunito(
                      fontSize: 10,
                      fontWeight: FontWeight.w700,
                      color: const Color(0xFFAAAACC),
                    ),
                  ),
                ],
              ),
            ),
            Container(
              width: 1,
              height: 38,
              margin: EdgeInsets.only(left: 7),
              color: const Color(0xFFEEEAFF),
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
                        color: const Color(0xFF9B8FFF),
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
                      const Icon(
                        Icons.lock_outline_rounded,
                        size: 10,
                        color: Color(0xFFAAAACC),
                      ),
                      const SizedBox(width: 3),
                      Text(
                        'Locked until ${_fmt(entry.startTime)}',
                        style: GoogleFonts.nunito(
                          fontSize: 10,
                          fontWeight: FontWeight.w700,
                          color: const Color(0xFFAAAACC),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(width: 8),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
              decoration: BoxDecoration(
                color: const Color(0xFFF0ECFF),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Text(
                countdown,
                style: GoogleFonts.nunito(
                  fontSize: 10,
                  fontWeight: FontWeight.w900,
                  color: const Color(0xFF6355EE),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  String _countdown() {
    final now = TimeOfDay.now();
    final nowMins = now.hour * 60 + now.minute;
    final startMins = entry.startTime.hour * 60 + entry.startTime.minute;
    final mins = startMins - nowMins;
    if (mins <= 0) return 'now';
    if (mins < 60) return 'in ${mins}m';
    final h = mins ~/ 60;
    final m = mins % 60;
    return m > 0 ? 'in ${h}h ${m}m' : 'in ${h}h';
  }

  String _fmt(TimeOfDay t) {
    final h = t.hourOfPeriod == 0 ? 12 : t.hourOfPeriod;
    final m = t.minute.toString().padLeft(2, '0');
    final p = t.period == DayPeriod.am ? 'AM' : 'PM';
    return '$h:$m $p';
  }
}
