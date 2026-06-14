import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../data/models/goal_models.dart';
import 'goal_status_chip.dart';
import 'goal_type_chip.dart';

class GoalCardTopRow extends StatelessWidget {
  final GoalListItem goal;

  const GoalCardTopRow({super.key, required this.goal});

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          width: 38,
          height: 38,
          decoration: BoxDecoration(
            color: _iconBg(),
            borderRadius: BorderRadius.circular(12),
          ),
          child: Center(
            child: Text(_icon(), style: const TextStyle(fontSize: 18)),
          ),
        ),
        const SizedBox(width: 10),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                goal.title,
                style: GoogleFonts.nunito(
                  fontSize: 14,
                  fontWeight: FontWeight.w900,
                  color: const Color(0xFF1A1A2E),
                ),
              ),
              if (goal.intentionTitle.isNotEmpty) ...[
                const SizedBox(height: 2),
                Text(
                  goal.intentionTitle,
                  style: GoogleFonts.nunito(
                    fontSize: 11,
                    fontWeight: FontWeight.w700,
                    color: const Color(0xFF9090AA),
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
              const SizedBox(height: 4),
              Wrap(
                spacing: 6,
                runSpacing: 4,
                children: [
                  GoalStatusChip(goal.status),
                  GoalTypeChip(goal.type),
                ],
              ),
            ],
          ),
        ),
      ],
    );
  }

  String _icon() => switch (goal.type) {
        GoalType.habit => '🔥',
        GoalType.abstinence => '🧊',
        GoalType.project => '🚀',
        GoalType.quantified => '📚',
        _ => '✅',
      };

  Color _iconBg() => switch (goal.type) {
        GoalType.habit => const Color(0xFF7B6EFF).withValues(alpha: 0.14),
        GoalType.abstinence => const Color(0xFFFF6B6B).withValues(alpha: 0.12),
        GoalType.project => const Color(0xFF7BB3FF).withValues(alpha: 0.15),
        GoalType.quantified => const Color(0xFFFFB800).withValues(alpha: 0.14),
        _ => const Color(0xFF5BBFB8).withValues(alpha: 0.15),
      };
}
