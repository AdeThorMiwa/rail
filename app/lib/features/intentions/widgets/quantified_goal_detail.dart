import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import '../data/models/goal_models.dart';
import '../providers/goals_provider.dart';
import 'goal_sect_label.dart';
import 'quant_hero.dart';
import 'quant_milestone_row.dart';
import 'quant_progress_bar.dart';
import 'task_timeline_row.dart';

class QuantifiedGoalDetail extends ConsumerStatefulWidget {
  final GoalDetail goal;

  const QuantifiedGoalDetail({super.key, required this.goal});

  @override
  ConsumerState<QuantifiedGoalDetail> createState() => _QuantifiedGoalDetailState();
}

class _QuantifiedGoalDetailState extends ConsumerState<QuantifiedGoalDetail> {
  bool _completing = false;

  Future<void> _markComplete() async {
    setState(() => _completing = true);
    try {
      await ref.read(goalsRepositoryProvider).completeGoal(widget.goal.pid);
      ref.invalidate(goalsProvider);
      if (mounted) context.pop();
    } finally {
      if (mounted) setState(() => _completing = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final targetData = widget.goal.target;
    final current = targetData?.currentValue ?? widget.goal.actualTotalHours.toDouble();
    final target = targetData?.targetValue ?? widget.goal.estimatedTotalHours.toDouble();
    final unit = targetData?.unit ?? 'hrs';
    final pct = target > 0 ? (current / target).clamp(0.0, 1.0) : 0.0;
    final pctLabel = target == 0 ? '—' : '${(pct * 100).round()}%';

    return SliverList(
      delegate: SliverChildListDelegate([
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 40),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              QuantHero(current: current, target: target, unit: unit),
              QuantProgressBar(pct: pct, pctLabel: pctLabel),
              if (pct >= 1.0) ...[
                const SizedBox(height: 20),
                SizedBox(
                  width: double.infinity,
                  child: DecoratedBox(
                    decoration: BoxDecoration(
                      gradient: const LinearGradient(
                        colors: [Color(0xFF7B6EFF), Color(0xFFB57BFF)],
                      ),
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: ElevatedButton(
                      onPressed: _completing ? null : _markComplete,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.transparent,
                        shadowColor: Colors.transparent,
                        padding: const EdgeInsets.symmetric(vertical: 16),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(16),
                        ),
                      ),
                      child: _completing
                          ? const SizedBox(
                              width: 20,
                              height: 20,
                              child: CircularProgressIndicator(
                                color: Colors.white,
                                strokeWidth: 2,
                              ),
                            )
                          : Text(
                              'Mark Goal Complete 🎉',
                              style: GoogleFonts.nunito(
                                fontSize: 16,
                                fontWeight: FontWeight.w800,
                                color: Colors.white,
                              ),
                            ),
                    ),
                  ),
                ),
              ],
              const SizedBox(height: 16),
              if (widget.goal.orphanTasks.isNotEmpty) ...[
                const GoalSectLabel(label: 'Tasks'),
                ...widget.goal.orphanTasks.map((t) => TaskTimelineRow(task: t)),
                const SizedBox(height: 20),
              ],
              if (widget.goal.milestones.isNotEmpty) ...[
                const GoalSectLabel(label: 'Milestones'),
                ...widget.goal.milestones.map((m) => QuantMilestoneRow(milestone: m)),
              ],
            ],
          ),
        ),
      ]),
    );
  }
}
