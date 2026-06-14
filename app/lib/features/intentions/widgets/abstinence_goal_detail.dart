import 'package:flutter/material.dart';
import '../data/models/goal_models.dart';
import 'abstinence_slip_button.dart';
import 'abstinence_streak_hero.dart';
import 'goal_month_grid.dart';
import 'goal_sect_label.dart';
import 'goal_stats_grid.dart';
import 'goal_week_card.dart';

class AbstinenceGoalDetail extends StatelessWidget {
  final GoalDetail goal;

  const AbstinenceGoalDetail({super.key, required this.goal});

  @override
  Widget build(BuildContext context) {
    final stats = goal.habitStats ?? HabitStats.empty();
    final weekDays = _buildWeekDays(stats.weekDotStatuses);
    final monthDays = _buildMonthDays(stats.monthDotStatuses);

    final allTimePct = '${(stats.allTimeRate * 100).round()}%';

    return SliverList(
      delegate: SliverChildListDelegate([
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 40),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              AbstinenceStreakHero(daysClean: stats.currentStreak),
              GoalWeekCard(
                title: 'This week',
                days: weekDays,
                dotColor: const Color(0xFFFF6B6B),
              ),
              GoalStatsGrid(
                stats: [
                  GoalStatItem(
                    value: '${stats.currentStreak}d',
                    label: 'Current streak',
                    valueColor: const Color(0xFFFF6B6B),
                  ),
                  GoalStatItem(
                    value: '${stats.bestStreak}d',
                    label: 'Best streak',
                    valueColor: const Color(0xFF7B6EFF),
                  ),
                  GoalStatItem(
                    value: allTimePct,
                    label: 'Clean rate',
                    valueColor: const Color(0xFF5BBFB8),
                  ),
                  GoalStatItem(
                    value: '${stats.slipsTotal}',
                    label: 'Slips total',
                  ),
                ],
              ),
              const SizedBox(height: 16),
              const GoalSectLabel(label: 'Last 30 days'),
              GoalMonthGrid(
                days: monthDays,
                doneColor: const Color(0xFF5BBFB8),
                missColor: const Color(0xFFFFD0D0),
              ),
              const SizedBox(height: 20),
              AbstinenceSlipButton(goalPid: goal.pid),
            ],
          ),
        ),
      ]),
    );
  }

  List<({DayStatus status, String label})> _buildWeekDays(List<String> statuses) {
    const labels = ['M', 'T', 'W', 'T', 'F', 'S', 'S'];
    final days = <({DayStatus status, String label})>[];
    for (int i = 0; i < 7; i++) {
      final raw = i < statuses.length ? statuses[i] : 'UPCOMING';
      final status = switch (raw) {
        'DONE' => DayStatus.done,
        'MISS' => DayStatus.missed,
        'TODAY' => DayStatus.today,
        _ => DayStatus.upcoming,
      };
      days.add((status: status, label: labels[i]));
    }
    return days;
  }

  List<bool?> _buildMonthDays(List<String> statuses) {
    return statuses.map((s) => switch (s) {
          'DONE' => true,
          'MISS' => false,
          _ => null,
        }).toList();
  }
}
