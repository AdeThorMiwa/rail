import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../data/models/goal_models.dart';

class GoalTypeChip extends StatelessWidget {
  final GoalType type;

  const GoalTypeChip(this.type, {super.key});

  @override
  Widget build(BuildContext context) {
    final (label, bg, fg) = switch (type) {
      GoalType.habit => (
          'Habit',
          const Color(0xFF7B6EFF).withValues(alpha: 0.12),
          const Color(0xFF7B6EFF),
        ),
      GoalType.abstinence => (
          'Abstinence',
          const Color(0xFFFF6B6B).withValues(alpha: 0.10),
          const Color(0xFFFF6B6B),
        ),
      GoalType.project => (
          'Project',
          const Color(0xFF7BB3FF).withValues(alpha: 0.12),
          const Color(0xFF4D9FFF),
        ),
      GoalType.quantified => (
          'Quantified',
          const Color(0xFFFFB800).withValues(alpha: 0.12),
          const Color(0xFFC48000),
        ),
      GoalType.task => (
          'Task',
          const Color(0xFF5BBFB8).withValues(alpha: 0.12),
          const Color(0xFF2EA69F),
        ),
    };

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(
        label,
        style: GoogleFonts.nunito(
          fontSize: 10,
          fontWeight: FontWeight.w900,
          color: fg,
          letterSpacing: 0.5,
        ),
      ),
    );
  }
}
