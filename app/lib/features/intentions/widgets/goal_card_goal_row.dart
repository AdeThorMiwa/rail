import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../data/models/goal_models.dart';

class GoalCardGoalRow extends StatelessWidget {
  final GoalListItem goal;

  const GoalCardGoalRow({super.key, required this.goal});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      decoration: BoxDecoration(
        color: const Color(0xFFF4F8FF),
        borderRadius: BorderRadius.circular(14),
      ),
      child: Row(
        children: [
          Container(
            width: 8,
            height: 8,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              gradient: goal.type == GoalType.abstinence
                  ? null
                  : const LinearGradient(
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                      colors: [Color(0xFF7B6EFF), Color(0xFF5BBFB8)],
                    ),
              color: goal.type == GoalType.abstinence
                  ? const Color(0xFFFF6B6B)
                  : null,
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              _goalText(),
              style: GoogleFonts.nunito(
                fontSize: 12,
                fontWeight: FontWeight.w800,
                color: const Color(0xFF1A1A2E),
              ),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ),
          const SizedBox(width: 8),
          Text(
            _countText(),
            style: GoogleFonts.nunito(
              fontSize: 11,
              fontWeight: FontWeight.w700,
              color: const Color(0xFF9090AA),
            ),
          ),
        ],
      ),
    );
  }

  String _goalText() => switch (goal.type) {
        GoalType.project => goal.currentMilestoneTitle ?? goal.title,
        GoalType.quantified => goal.firstPendingTaskTitle ?? goal.title,
        GoalType.abstinence => '${goal.title} · ongoing',
        _ => goal.title,
      };

  String _countText() {
    final count = goal.pendingTaskCount;
    if (count == 0) return 'no tasks today';
    return '$count task${count == 1 ? '' : 's'} today';
  }
}
