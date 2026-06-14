import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../data/models/goal_models.dart';

class GoalCardProjectFooter extends StatelessWidget {
  final GoalListItem goal;

  const GoalCardProjectFooter({super.key, required this.goal});

  @override
  Widget build(BuildContext context) {
    final pct = goal.milestoneProgress;
    final pctLabel =
        goal.milestonesTotal == 0 ? '—' : '${(pct * 100).round()}%';

    return Padding(
      padding: const EdgeInsets.only(top: 8),
      child: Row(
        children: [
          Expanded(
            child: ClipRRect(
              borderRadius: BorderRadius.circular(3),
              child: Container(
                height: 5,
                color: const Color(0xFFEEE8FF),
                child: FractionallySizedBox(
                  alignment: Alignment.centerLeft,
                  widthFactor: pct,
                  child: Container(
                    decoration: const BoxDecoration(
                      gradient: LinearGradient(
                        colors: [Color(0xFF7BB3FF), Color(0xFF4D9FFF)],
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
          const SizedBox(width: 10),
          Text(
            pctLabel,
            style: GoogleFonts.nunito(
              fontSize: 11,
              fontWeight: FontWeight.w900,
              color: const Color(0xFF4D9FFF),
            ),
          ),
        ],
      ),
    );
  }
}
