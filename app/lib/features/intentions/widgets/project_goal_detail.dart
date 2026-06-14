import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../data/models/goal_models.dart';
import 'goal_complete_cta.dart';
import 'milestone_timeline_item.dart';

class ProjectGoalDetail extends StatelessWidget {
  final GoalDetail goal;

  const ProjectGoalDetail({super.key, required this.goal});

  @override
  Widget build(BuildContext context) {
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
                return Padding(
                  padding: const EdgeInsets.only(bottom: 2),
                  child: IntrinsicHeight(
                    child: MilestoneTimelineItem(
                      milestone: goal.milestones[i],
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
