import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class ScheduleProgressRow extends StatelessWidget {
  final int done;
  final int total;
  final bool allDone;

  const ScheduleProgressRow({
    super.key,
    required this.done,
    required this.total,
    this.allDone = false,
  });

  @override
  Widget build(BuildContext context) {
    final progress = total == 0 ? 0.0 : done / total;

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 10),
      child: Row(
        children: [
          Expanded(
            child: ClipRRect(
              borderRadius: BorderRadius.circular(4),
              child: LinearProgressIndicator(
                value: progress,
                minHeight: 6,
                backgroundColor: const Color(0xFFE8E4FF),
                valueColor: AlwaysStoppedAnimation<Color>(
                  allDone ? const Color(0xFF22C55E) : const Color(0xFF6355EE),
                ),
              ),
            ),
          ),
          const SizedBox(width: 10),
          Text(
            allDone ? '$done / $total ✓' : '$done / $total done',
            style: GoogleFonts.nunito(
              fontSize: 12,
              fontWeight: FontWeight.w800,
              color: allDone ? const Color(0xFF22C55E) : const Color(0xFF6355EE),
            ),
          ),
        ],
      ),
    );
  }
}
