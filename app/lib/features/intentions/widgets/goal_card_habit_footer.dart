import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../data/models/goal_models.dart';

class GoalCardHabitFooter extends StatelessWidget {
  final HabitStats? habitStats;

  const GoalCardHabitFooter({super.key, this.habitStats});

  @override
  Widget build(BuildContext context) {
    final streak = habitStats?.currentStreak ?? 0;
    final dots = habitStats?.weekDotStatuses ?? List.filled(7, 'UPCOMING');

    return Padding(
      padding: const EdgeInsets.only(top: 10),
      child: Row(
        children: [
          Text(
            '🔥 $streak days',
            style: GoogleFonts.nunito(
              fontSize: 12,
              fontWeight: FontWeight.w900,
              color: const Color(0xFF7B6EFF),
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: dots.take(7).map((s) => _WeekDot(status: s, color: const Color(0xFF7B6EFF))).toList(),
            ),
          ),
        ],
      ),
    );
  }
}

class _WeekDot extends StatelessWidget {
  final String status;
  final Color color;

  const _WeekDot({required this.status, required this.color});

  @override
  Widget build(BuildContext context) {
    final isDone = status == 'DONE';
    final isMiss = status == 'MISS';
    return Container(
      width: 10,
      height: 10,
      margin: const EdgeInsets.symmetric(horizontal: 2),
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: isDone
            ? color
            : isMiss
                ? const Color(0xFFFFE0E0)
                : const Color(0xFFEEEAFF),
      ),
    );
  }
}
