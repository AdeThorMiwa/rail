import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

enum DayStatus { done, missed, today, upcoming }

class GoalWeekCard extends StatelessWidget {
  final String title;
  final List<({DayStatus status, String label})> days;
  final Color dotColor;

  const GoalWeekCard({
    super.key,
    required this.title,
    required this.days,
    this.dotColor = const Color(0xFF7B6EFF),
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 14, 16, 14),
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.05),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: GoogleFonts.nunito(
              fontSize: 12,
              fontWeight: FontWeight.w900,
              color: const Color(0xFFAAAAC0),
              letterSpacing: 0.6,
            ),
          ),
          const SizedBox(height: 12),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: days
                .map((d) => Column(
                      children: [
                        _DayDot(status: d.status, color: dotColor),
                        const SizedBox(height: 5),
                        Text(
                          d.label,
                          style: GoogleFonts.nunito(
                            fontSize: 10,
                            fontWeight: FontWeight.w900,
                            color: const Color(0xFFAAAAC0),
                          ),
                        ),
                      ],
                    ))
                .toList(),
          ),
        ],
      ),
    );
  }
}

class _DayDot extends StatelessWidget {
  final DayStatus status;
  final Color color;

  const _DayDot({required this.status, required this.color});

  @override
  Widget build(BuildContext context) {
    final isDone = status == DayStatus.done || status == DayStatus.today;
    final isToday = status == DayStatus.today;
    final isMissed = status == DayStatus.missed;

    return Container(
      width: 26,
      height: 26,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        gradient: isDone
            ? LinearGradient(
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: [color, color.withValues(alpha: 0.7)],
              )
            : null,
        color: !isDone
            ? (isMissed ? const Color(0xFFFFE0E0) : const Color(0xFFF0ECFF))
            : null,
        boxShadow: isToday
            ? [
                BoxShadow(
                  color: color.withValues(alpha: 0.25),
                  blurRadius: 0,
                  spreadRadius: 3,
                )
              ]
            : null,
      ),
    );
  }
}
