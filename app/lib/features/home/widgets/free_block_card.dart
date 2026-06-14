import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../data/models/schedule_models.dart';

class FreeBlockCard extends StatelessWidget {
  final ScheduleEntry entry;

  const FreeBlockCard({super.key, required this.entry});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [Color(0xFFE8F8F7), Color(0xFFF0FBFA)],
        ),
        border: Border.all(color: const Color(0xFFC4EDE9), width: 1.5),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        children: [
          Text('🍃', style: const TextStyle(fontSize: 22)),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(entry.notes ?? 'Free time',
                    style: GoogleFonts.nunito(
                        fontSize: 14,
                        fontWeight: FontWeight.w800,
                        color: const Color(0xFF2EA69F))),
                const SizedBox(height: 2),
                Text(
                    '${_fmt(entry.startTime)} – ${_fmt(entry.endTime)}',
                    style: GoogleFonts.nunito(
                        fontSize: 11,
                        fontWeight: FontWeight.w700,
                        color: const Color(0xFF8CCED9))),
              ],
            ),
          ),
          Text('${entry.durationMinutes}m',
              style: GoogleFonts.nunito(
                  fontSize: 12,
                  fontWeight: FontWeight.w900,
                  color: const Color(0xFF5BBFB8))),
        ],
      ),
    );
  }

  String _fmt(TimeOfDay t) {
    final h = t.hourOfPeriod == 0 ? 12 : t.hourOfPeriod;
    final m = t.minute.toString().padLeft(2, '0');
    final p = t.period == DayPeriod.am ? 'AM' : 'PM';
    return '$h:$m $p';
  }
}
