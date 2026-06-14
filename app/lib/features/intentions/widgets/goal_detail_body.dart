import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../data/models/goal_models.dart';
import 'abstinence_goal_detail.dart';
import 'goal_detail_header.dart';
import 'habit_goal_detail.dart';
import 'project_goal_detail.dart';
import 'quantified_goal_detail.dart';

class GoalDetailBody extends StatelessWidget {
  final GoalDetail goal;
  final Future<void> Function()? onRefresh;

  const GoalDetailBody({super.key, required this.goal, this.onRefresh});

  @override
  Widget build(BuildContext context) {
    final scroll = CustomScrollView(
      physics: const AlwaysScrollableScrollPhysics(),
      slivers: [
        SliverToBoxAdapter(
          child: GoalDetailHeader(
            intentionTitle: goal.intentionTitle,
            goalTitle: goal.title,
            goalType: goal.type,
            onBack: () => context.pop(),
          ),
        ),
        _goalContent(),
      ],
    );

    if (onRefresh == null) return scroll;
    return RefreshIndicator(
      onRefresh: onRefresh!,
      color: const Color(0xFF7B6EFF),
      child: scroll,
    );
  }

  Widget _goalContent() => switch (goal.type) {
        GoalType.project => ProjectGoalDetail(goal: goal),
        GoalType.habit => HabitGoalDetail(goal: goal),
        GoalType.abstinence => AbstinenceGoalDetail(goal: goal),
        _ => QuantifiedGoalDetail(goal: goal),
      };
}
