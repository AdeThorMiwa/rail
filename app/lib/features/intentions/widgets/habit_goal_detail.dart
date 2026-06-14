import 'package:flutter/material.dart';
import '../data/models/goal_models.dart';
import 'goal_month_grid.dart';
import 'goal_sect_label.dart';
import 'goal_stats_grid.dart';
import 'goal_week_card.dart';
import 'habit_streak_hero.dart';
import 'task_timeline_row.dart';

class HabitGoalDetail extends StatelessWidget {
  final GoalDetail goal;

  const HabitGoalDetail({super.key, required this.goal});

  @override
  Widget build(BuildContext context) {
    final stats = goal.habitStats ?? HabitStats.empty();
    final weekDays = _buildWeekDays(stats.weekDotStatuses);
    final monthDays = _buildMonthDays(stats.monthDotStatuses);

    final doneCount = stats.thisWeekDone;
    final targetCount = stats.thisWeekTarget;
    final weekTitle = 'This week · $doneCount / $targetCount';

    final thisWeekPct = '${(stats.thisWeekRate * 100).round()}%';
    final allTimePct = '${(stats.allTimeRate * 100).round()}%';

    return SliverList(
      delegate: SliverChildListDelegate([
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 40),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              HabitStreakHero(streakDays: stats.currentStreak),
              GoalWeekCard(
                title: weekTitle,
                days: weekDays,
                dotColor: const Color(0xFF7B6EFF),
              ),
              GoalStatsGrid(
                stats: [
                  GoalStatItem(value: thisWeekPct, label: 'This week', valueColor: const Color(0xFF7B6EFF)),
                  GoalStatItem(value: allTimePct, label: 'All time', valueColor: const Color(0xFF5BBFB8)),
                  GoalStatItem(value: '${stats.bestStreak}', label: 'Best streak'),
                  GoalStatItem(value: '${stats.missedThisWeek}', label: 'Missed this week', valueColor: const Color(0xFFFF9F43)),
                ],
              ),
              const SizedBox(height: 16),
              const GoalSectLabel(label: 'Monthly view'),
              GoalMonthGrid(days: monthDays),
              if (goal.orphanTasks.isNotEmpty) ...[
                const SizedBox(height: 20),
                const GoalSectLabel(label: "Today's tasks"),
                ...goal.orphanTasks.map((t) => TaskTimelineRow(task: t)),
              ],
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
