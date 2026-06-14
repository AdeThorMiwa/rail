import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'empty_state_card.dart';

class EmptyDay extends StatelessWidget {
  final VoidCallback onConnieTapped;
  const EmptyDay({super.key, required this.onConnieTapped});

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 120),
      children: [
        const EmptyStateCard(),
        const SizedBox(height: 8),
        GestureDetector(
          onTap: onConnieTapped,
          child: Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(vertical: 14),
            decoration: BoxDecoration(
              gradient: const LinearGradient(
                  colors: [Color(0xFF7B6EFF), Color(0xFF9B8FFF)]),
              borderRadius: BorderRadius.circular(14),
            ),
            child: Center(
              child: Text(
                'Tell Connie what you want to do',
                style: GoogleFonts.nunito(
                    fontSize: 15,
                    fontWeight: FontWeight.w800,
                    color: Colors.white),
              ),
            ),
          ),
        ),
        const SizedBox(height: 12),
        Container(
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(12),
            boxShadow: [
              BoxShadow(
                  color: Colors.black.withValues(alpha: 0.04),
                  blurRadius: 6,
                  offset: const Offset(0, 2))
            ],
          ),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('💡', style: TextStyle(fontSize: 16)),
              const SizedBox(width: 10),
              Expanded(
                child: Text.rich(
                  TextSpan(
                    style: GoogleFonts.nunito(
                        fontSize: 12,
                        fontWeight: FontWeight.w700,
                        color: const Color(0xFF9090AA),
                        height: 1.6),
                    children: const [
                      TextSpan(text: 'You can say something like '),
                      TextSpan(
                          text: '"I want to ship my app"',
                          style: TextStyle(
                              color: Color(0xFF7B6EFF),
                              fontWeight: FontWeight.w800)),
                      TextSpan(text: ' or '),
                      TextSpan(
                          text: '"I want to get fit"',
                          style: TextStyle(
                              color: Color(0xFF7B6EFF),
                              fontWeight: FontWeight.w800)),
                      TextSpan(text: ' — Rail will figure out the rest.'),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}
