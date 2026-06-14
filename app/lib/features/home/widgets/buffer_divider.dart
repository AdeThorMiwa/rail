import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class BufferDivider extends StatelessWidget {
  final int minutes;

  const BufferDivider({super.key, required this.minutes});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        children: [
          Expanded(
              child: Container(height: 1, color: const Color(0xFFE2DFF5))),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 8),
            child: Text('·· $minutes min buffer ··',
                style: GoogleFonts.nunito(
                  fontSize: 10,
                  fontWeight: FontWeight.w900,
                  color: const Color(0xFFCACAE0),
                  letterSpacing: 0.5,
                )),
          ),
          Expanded(
              child: Container(height: 1, color: const Color(0xFFE2DFF5))),
        ],
      ),
    );
  }
}
