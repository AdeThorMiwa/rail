import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class EmptyIntentions extends StatelessWidget {
  final VoidCallback onAdd;

  const EmptyIntentions({super.key, required this.onAdd});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(24),
      child: Column(
        children: [
          const Text('🎯', style: TextStyle(fontSize: 48)),
          const SizedBox(height: 16),
          Text('No intentions yet',
              style: GoogleFonts.nunito(
                  fontSize: 18,
                  fontWeight: FontWeight.w900,
                  color: const Color(0xFF1A1A2E))),
          const SizedBox(height: 8),
          Text(
            'Head back to Connie and tell her what you want to work towards.',
            textAlign: TextAlign.center,
            style: GoogleFonts.nunito(
                fontSize: 13,
                fontWeight: FontWeight.w600,
                color: const Color(0xFF9090AA),
                height: 1.6),
          ),
          const SizedBox(height: 24),
          GestureDetector(
            onTap: onAdd,
            child: Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(vertical: 14),
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                    colors: [Color(0xFF7B6EFF), Color(0xFF9B8FFF)]),
                borderRadius: BorderRadius.circular(14),
              ),
              child: Center(
                child: Text('Back to Connie 🎩',
                    style: GoogleFonts.nunito(
                        fontSize: 15,
                        fontWeight: FontWeight.w900,
                        color: Colors.white)),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
