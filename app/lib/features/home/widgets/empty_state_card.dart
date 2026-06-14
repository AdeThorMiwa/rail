import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class EmptyStateCard extends StatelessWidget {
  const EmptyStateCard({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.symmetric(vertical: 12),
      padding: const EdgeInsets.fromLTRB(24, 36, 24, 28),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFFF0ECFF), Color(0xFFEAF0FF)],
        ),
        border: Border.all(color: const Color(0xFFE2DCFF), width: 1.5),
        borderRadius: BorderRadius.circular(18),
      ),
      child: Column(
        children: [
          const Text('🎩', style: TextStyle(fontSize: 52)),
          const SizedBox(height: 16),
          Text('Your board is empty',
              style: GoogleFonts.nunito(
                fontSize: 20,
                fontWeight: FontWeight.w900,
                color: const Color(0xFF1A1A2E),
              )),
          const SizedBox(height: 10),
          Text(
            "Rail doesn't know what you're working towards yet. Tell me what matters to you and I'll build your schedule around it.",
            textAlign: TextAlign.center,
            style: GoogleFonts.nunito(
              fontSize: 13,
              fontWeight: FontWeight.w600,
              color: const Color(0xFF6060A0),
              height: 1.65,
            ),
          ),
        ],
      ),
    );
  }
}
