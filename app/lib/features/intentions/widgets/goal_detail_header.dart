import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../data/models/goal_models.dart';
import 'goal_type_chip.dart';

class GoalDetailHeader extends StatelessWidget {
  final String intentionTitle;
  final String goalTitle;
  final GoalType goalType;
  final VoidCallback onBack;

  const GoalDetailHeader({
    super.key,
    required this.intentionTitle,
    required this.goalTitle,
    required this.goalType,
    required this.onBack,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 52, 16, 0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          GestureDetector(
            onTap: onBack,
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(
                  Icons.chevron_left_rounded,
                  size: 18,
                  color: Color(0xFF9090AA),
                ),
                Text(
                  'My Goals',
                  style: GoogleFonts.nunito(
                    fontSize: 13,
                    fontWeight: FontWeight.w800,
                    color: const Color(0xFF9090AA),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),
          Text(
            goalTitle,
            style: GoogleFonts.nunito(
              fontSize: 22,
              fontWeight: FontWeight.w900,
              color: const Color(0xFF1A1A2E),
              height: 1.25,
            ),
          ),
          const SizedBox(height: 6),
          Row(
            children: [
              GoalTypeChip(goalType),
              const SizedBox(width: 8),
              Flexible(
                child: Text(
                  intentionTitle,
                  overflow: TextOverflow.ellipsis,
                  style: GoogleFonts.nunito(
                    fontSize: 12,
                    fontWeight: FontWeight.w700,
                    color: const Color(0xFF9090AA),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
