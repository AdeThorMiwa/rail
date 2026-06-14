import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../data/models/goal_models.dart';

class GoalCardAbstinenceFooter extends StatelessWidget {
  final HabitStats? habitStats;

  const GoalCardAbstinenceFooter({super.key, this.habitStats});

  @override
  Widget build(BuildContext context) {
    final streak = habitStats?.currentStreak ?? 0;
    final dots = habitStats?.weekDotStatuses ?? List.filled(7, 'UPCOMING');

    return Padding(
      padding: const EdgeInsets.only(top: 10),
      child: Row(
        children: [
          Text(
            '🧊 $streak days clean',
            style: GoogleFonts.nunito(
              fontSize: 12,
              fontWeight: FontWeight.w900,
              color: const Color(0xFFFF6B6B),
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: dots.take(7).map((s) => _WeekDot(status: s)).toList(),
            ),
          ),
        ],
      ),
    );
  }
}

class _WeekDot extends StatelessWidget {
  final String status;

  const _WeekDot({required this.status});

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
            ? const Color(0xFF5BBFB8)
            : isMiss
                ? const Color(0xFFFFE0E0)
                : const Color(0xFFFFE8E8),
      ),
    );
  }
}
