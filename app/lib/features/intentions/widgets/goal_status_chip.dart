import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../data/models/goal_models.dart';

class GoalStatusChip extends StatelessWidget {
  final GoalStatus status;

  const GoalStatusChip(this.status, {super.key});

  @override
  Widget build(BuildContext context) {
    final (label, bg, fg) = switch (status) {
      GoalStatus.active => (
          'Active',
          const Color(0xFF3AAFA9).withValues(alpha: 0.16),
          const Color(0xFF3AAFA9),
        ),
      GoalStatus.blocked => (
          'Blocked',
          const Color(0xFFFF9F43).withValues(alpha: 0.15),
          const Color(0xFFC47000),
        ),
      GoalStatus.paused => (
          'Paused',
          const Color(0xFFFFC107).withValues(alpha: 0.16),
          const Color(0xFFC48000),
        ),
      GoalStatus.completed => (
          'Done',
          const Color(0xFF22C55E).withValues(alpha: 0.14),
          const Color(0xFF22A65A),
        ),
      GoalStatus.abandoned => (
          'Abandoned',
          const Color(0xFF9090AA).withValues(alpha: 0.12),
          const Color(0xFF9090AA),
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
