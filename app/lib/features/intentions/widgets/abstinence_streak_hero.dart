import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class AbstinenceStreakHero extends StatelessWidget {
  final int daysClean;
  final String? sinceDateLabel;

  const AbstinenceStreakHero({
    super.key,
    this.daysClean = 0,
    this.sinceDateLabel,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(0, 24, 0, 18),
      child: Center(
        child: Column(
          children: [
            const Text('🧊', style: TextStyle(fontSize: 40, height: 1)),
            const SizedBox(height: 6),
            Text(
              '$daysClean',
              style: GoogleFonts.nunito(
                fontSize: 56,
                fontWeight: FontWeight.w900,
                color: const Color(0xFFFF6B6B),
                height: 1,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              'days clean',
              style: GoogleFonts.nunito(
                fontSize: 14,
                fontWeight: FontWeight.w800,
                color: const Color(0xFF9090AA),
              ),
            ),
            if (sinceDateLabel != null) ...[
              const SizedBox(height: 3),
              Text(
                sinceDateLabel!,
                style: GoogleFonts.nunito(
                  fontSize: 11,
                  fontWeight: FontWeight.w700,
                  color: const Color(0xFFC8C0DC),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
