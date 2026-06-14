import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class QuantProgressBar extends StatelessWidget {
  final double pct;
  final String pctLabel;

  const QuantProgressBar({
    super.key,
    required this.pct,
    required this.pctLabel,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: ClipRRect(
            borderRadius: BorderRadius.circular(4),
            child: Container(
              height: 8,
              color: const Color(0xFFFFF0C4),
              child: FractionallySizedBox(
                alignment: Alignment.centerLeft,
                widthFactor: pct,
                child: Container(
                  decoration: const BoxDecoration(
                    gradient: LinearGradient(
                      colors: [Color(0xFFFFB800), Color(0xFFFFDA6B)],
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
        const SizedBox(width: 10),
        SizedBox(
          width: 36,
          child: Text(
            pctLabel,
            textAlign: TextAlign.right,
            style: GoogleFonts.nunito(
              fontSize: 13,
              fontWeight: FontWeight.w900,
              color: const Color(0xFFC48000),
            ),
          ),
        ),
      ],
    );
  }
}
