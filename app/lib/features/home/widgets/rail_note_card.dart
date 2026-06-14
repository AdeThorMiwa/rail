import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class RailNoteCard extends StatelessWidget {
  final String note;

  const RailNoteCard({super.key, required this.note});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(top: 12, bottom: 4),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFFF0ECFF), Color(0xFFEAF0FF)],
        ),
        border: Border.all(color: const Color(0xFFE2DCFF), width: 1.5),
        borderRadius: BorderRadius.circular(14),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 32,
            height: 32,
            decoration: const BoxDecoration(
              gradient: LinearGradient(
                colors: [Color(0xFF7B6EFF), Color(0xFF9B8FFF)],
              ),
              shape: BoxShape.circle,
            ),
            child: const Center(
                child: Text('🎩', style: TextStyle(fontSize: 14))),
          ),
          const SizedBox(width: 11),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Rail\'s note',
                    style: GoogleFonts.nunito(
                      fontSize: 10,
                      fontWeight: FontWeight.w900,
                      color: const Color(0xFF9B8FFF),
                      letterSpacing: 0.8,
                    )),
                const SizedBox(height: 4),
                Text(note,
                    style: GoogleFonts.nunito(
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                      color: const Color(0xFF3A3558),
                      height: 1.6,
                    )),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
