import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import '../data/models/schedule_models.dart';

class SkippedEntryCard extends StatelessWidget {
  final ScheduleEntry entry;

  const SkippedEntryCard({super.key, required this.entry});

  @override
  Widget build(BuildContext context) {
    final task = entry.task!;
    final isMissed = entry.isMissed;
    final badgeColor = isMissed ? const Color(0xFFFF6B6B) : const Color(0xFFAAAAC0);
    final badgeLabel = isMissed ? 'MISSED' : 'SKIPPED';

    return GestureDetector(
      onTap: () => context.push(
        '/chat/TASK/${task.pid}?title=${Uri.encodeComponent(task.title)}',
      ),
      child: Container(
        margin: const EdgeInsets.only(bottom: 10),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: const Color(0xFFEEEEF5), width: 1.5),
        ),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      task.title,
                      style: GoogleFonts.nunito(
                        fontSize: 15,
                        fontWeight: FontWeight.w700,
                        color: const Color(0xFFAAAAC0),
                        decoration: TextDecoration.lineThrough,
                        decorationColor: const Color(0xFFAAAAC0),
                      ),
                    ),
                    if (entry.skipReason != null && entry.skipReason!.isNotEmpty) ...[
                      const SizedBox(height: 4),
                      Text(
                        entry.skipReason!,
                        style: GoogleFonts.nunito(
                          fontSize: 12,
                          fontWeight: FontWeight.w600,
                          color: const Color(0xFFCCCCDD),
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              const SizedBox(width: 10),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(
                  color: badgeColor.withValues(alpha: 0.1),
                  borderRadius: BorderRadius.circular(5),
                ),
                child: Text(
                  badgeLabel,
                  style: GoogleFonts.nunito(
                    fontSize: 10,
                    fontWeight: FontWeight.w900,
                    color: badgeColor,
                    letterSpacing: 0.4,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
