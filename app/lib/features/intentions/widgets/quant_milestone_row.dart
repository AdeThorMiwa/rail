import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../data/models/goal_models.dart';

class QuantMilestoneRow extends StatelessWidget {
  final MilestoneDetail milestone;

  const QuantMilestoneRow({super.key, required this.milestone});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Row(
        children: [
          Container(
            width: 10,
            height: 10,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              gradient: milestone.isDone
                  ? const LinearGradient(
                      colors: [Color(0xFF7B6EFF), Color(0xFF9B8FFF)],
                    )
                  : null,
              color: milestone.isDone ? null : const Color(0xFFEEE8FF),
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              milestone.title,
              style: GoogleFonts.nunito(
                fontSize: 13,
                fontWeight: FontWeight.w700,
                color: milestone.isDone
                    ? const Color(0xFF9090AA)
                    : const Color(0xFF1A1A2E),
                decoration:
                    milestone.isDone ? TextDecoration.lineThrough : null,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
