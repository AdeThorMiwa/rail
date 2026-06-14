import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class SchedulingHeader extends StatelessWidget {
  const SchedulingHeader({super.key});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 40),
      child: Center(
        child: Column(
          children: [
            const Text('🚂', style: TextStyle(fontSize: 38)),
            const SizedBox(height: 10),
            Text(
              'One last thing',
              style: GoogleFonts.nunito(
                fontSize: 24,
                fontWeight: FontWeight.w900,
                color: const Color(0xFF1A1A2E),
              ),
            ),
            const SizedBox(height: 8),
            Text(
              'Help me understand your rhythm so I can\nbuild a schedule that actually works for you.',
              textAlign: TextAlign.center,
              style: GoogleFonts.nunito(
                fontSize: 14,
                fontWeight: FontWeight.w600,
                color: const Color(0xFF9090AA),
                height: 1.65,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
