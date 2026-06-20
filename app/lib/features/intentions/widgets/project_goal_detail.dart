import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import '../data/models/goal_models.dart';
import '../providers/goals_provider.dart';
import 'goal_complete_cta.dart';
import 'milestone_timeline_item.dart';
import 'task_timeline_row.dart';

class ProjectGoalDetail extends ConsumerWidget {
  final GoalDetail goal;

  const ProjectGoalDetail({super.key, required this.goal});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tasksAsync = ref.watch(goalTasksProvider(goal.pid));

    final milestones = tasksAsync.whenData((tasks) {
      return goal.milestones.map((m) {
        return MilestoneDetail(
          pid: m.pid,
          title: m.title,
          status: m.status,
          tasks: tasks.where((t) => t.milestonePid == m.pid).toList(),
        );
      }).toList();
    });

    final orphanTasks = tasksAsync.whenData(
      (tasks) => tasks.where((t) => t.milestonePid == null).toList(),
    );

    final allDone = goal.milestones.isNotEmpty &&
        goal.milestones.every((m) => m.isDone);

    return SliverList(
      delegate: SliverChildListDelegate([
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 22, 16, 0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              ...List.generate(goal.milestones.length, (i) {
                final milestone = milestones.valueOrNull?[i] ?? goal.milestones[i];
                return Padding(
                  padding: const EdgeInsets.only(bottom: 2),
                  child: IntrinsicHeight(
                    child: MilestoneTimelineItem(
                      milestone: milestone,
                      isLast: i == goal.milestones.length - 1,
                    ),
                  ),
                );
              }),
              if (goal.milestones.isEmpty)
                Padding(
                  padding: const EdgeInsets.only(top: 24),
                  child: Center(
                    child: Text(
                      'No milestones yet',
                      style: GoogleFonts.nunito(
                        fontSize: 14,
                        fontWeight: FontWeight.w700,
                        color: const Color(0xFF9090AA),
                      ),
                    ),
                  ),
                ),
              if (orphanTasks.valueOrNull?.isNotEmpty == true) ...[
                const SizedBox(height: 20),
                Text(
                  'Other tasks',
                  style: GoogleFonts.nunito(
                    fontSize: 13,
                    fontWeight: FontWeight.w800,
                    color: const Color(0xFF9090AA),
                  ),
                ),
                const SizedBox(height: 10),
                ...orphanTasks.valueOrNull!.map((t) => TaskTimelineRow(task: t)),
              ],
              const SizedBox(height: 20),
              if (allDone) const GoalCompleteCta(),
              const SizedBox(height: 40),
            ],
          ),
        ),
      ]),
    );
  }
}
