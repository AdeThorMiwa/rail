import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class HabitStreakHero extends StatelessWidget {
  final int streakDays;

  const HabitStreakHero({super.key, this.streakDays = 0});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 24),
      child: Center(
        child: Column(
          children: [
            const Text('🔥', style: TextStyle(fontSize: 40, height: 1)),
            const SizedBox(height: 4),
            Text(
              '$streakDays',
              style: GoogleFonts.nunito(
                fontSize: 56,
                fontWeight: FontWeight.w900,
                color: const Color(0xFF1A1A2E),
                height: 1,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              'day streak',
              style: GoogleFonts.nunito(
                fontSize: 14,
                fontWeight: FontWeight.w800,
                color: const Color(0xFF9090AA),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
