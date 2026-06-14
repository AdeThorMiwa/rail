import 'package:flutter/material.dart';
import '../data/models/goal_models.dart';
import 'goal_card_abstinence_footer.dart';
import 'goal_card_goal_row.dart';
import 'goal_card_habit_footer.dart';
import 'goal_card_project_footer.dart';
import 'goal_card_quantified_footer.dart';
import 'goal_card_top_row.dart';

class GoalCard extends StatelessWidget {
  final GoalListItem goal;
  final VoidCallback? onTap;

  const GoalCard({super.key, required this.goal, this.onTap});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        margin: const EdgeInsets.only(bottom: 8),
        padding: const EdgeInsets.fromLTRB(14, 12, 14, 10),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(8),
          boxShadow: [
            BoxShadow(
              color: const Color(0xFF1E143C).withValues(alpha: 0.06),
              blurRadius: 10,
              offset: const Offset(0, 2),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            GoalCardTopRow(goal: goal),
            const SizedBox(height: 8),
            GoalCardGoalRow(goal: goal),
            _footer(),
          ],
        ),
      ),
    );
  }

  Widget _footer() => switch (goal.type) {
        GoalType.habit => GoalCardHabitFooter(habitStats: goal.habitStats),
        GoalType.abstinence => GoalCardAbstinenceFooter(habitStats: goal.habitStats),
        GoalType.project => GoalCardProjectFooter(goal: goal),
        GoalType.quantified => GoalCardQuantifiedFooter(goal: goal),
        _ => const SizedBox.shrink(),
      };
}
