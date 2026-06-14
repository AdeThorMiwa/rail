import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class QuantHero extends StatelessWidget {
  final double current;
  final double target;
  final String unit;

  const QuantHero({
    super.key,
    required this.current,
    required this.target,
    required this.unit,
  });

  static String _fmt(double v) =>
      v == v.truncateToDouble() ? v.toInt().toString() : v.toStringAsFixed(2);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(0, 24, 0, 10),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.baseline,
        textBaseline: TextBaseline.alphabetic,
        children: [
          Text(
            _fmt(current),
            style: GoogleFonts.nunito(
              fontSize: 64,
              fontWeight: FontWeight.w900,
              color: const Color(0xFFC48000),
              height: 1,
            ),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 6),
            child: Text(
              '/',
              style: GoogleFonts.nunito(
                fontSize: 28,
                fontWeight: FontWeight.w700,
                color: const Color(0xFFD8D4EE),
              ),
            ),
          ),
          Text(
            _fmt(target),
            style: GoogleFonts.nunito(
              fontSize: 36,
              fontWeight: FontWeight.w900,
              color: const Color(0xFF1A1A2E),
              height: 1,
            ),
          ),
          const SizedBox(width: 6),
          Text(
            unit,
            style: GoogleFonts.nunito(
              fontSize: 14,
              fontWeight: FontWeight.w800,
              color: const Color(0xFF9090AA),
            ),
          ),
        ],
      ),
    );
  }
}
