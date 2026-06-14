import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class GoalSectLabel extends StatelessWidget {
  final String label;

  const GoalSectLabel({super.key, required this.label});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Row(
        children: [
          Text(
            label.toUpperCase(),
            style: GoogleFonts.nunito(
              fontSize: 11,
              fontWeight: FontWeight.w900,
              color: const Color(0xFFAAAAC0),
              letterSpacing: 1,
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Container(
              height: 1.5,
              color: Colors.black.withValues(alpha: 0.06),
            ),
          ),
        ],
      ),
    );
  }
}
