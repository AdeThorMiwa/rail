import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../data/models/goal_models.dart';

class GoalCardQuantifiedFooter extends StatelessWidget {
  final GoalListItem goal;

  const GoalCardQuantifiedFooter({super.key, required this.goal});

  static String _fmt(double v) =>
      v == v.truncateToDouble() ? v.toInt().toString() : v.toStringAsFixed(2);

  @override
  Widget build(BuildContext context) {
    final targetData = goal.target;
    final current = targetData?.currentValue ?? goal.actualTotalHours.toDouble();
    final targetVal = targetData?.targetValue ?? goal.estimatedTotalHours.toDouble();
    final unit = targetData?.unit ?? 'hrs';
    final pct = targetVal > 0 ? (current / targetVal).clamp(0.0, 1.0) : 0.0;
    final pctLabel = targetVal == 0 ? '—' : '${(pct * 100).round()}%';

    return Padding(
      padding: const EdgeInsets.only(top: 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text(
                _fmt(current),
                style: GoogleFonts.nunito(
                  fontSize: 22,
                  fontWeight: FontWeight.w900,
                  color: const Color(0xFFC48000),
                ),
              ),
              Text(
                ' / ',
                style: GoogleFonts.nunito(
                  fontSize: 13,
                  fontWeight: FontWeight.w700,
                  color: const Color(0xFFC8C0E8),
                ),
              ),
              Text(
                '${_fmt(targetVal)} $unit',
                style: GoogleFonts.nunito(
                  fontSize: 13,
                  fontWeight: FontWeight.w800,
                  color: const Color(0xFFAAAAC0),
                ),
              ),
            ],
          ),
          const SizedBox(height: 6),
          Row(
            children: [
              Expanded(
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(3),
                  child: Container(
                    height: 5,
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
              Text(
                pctLabel,
                style: GoogleFonts.nunito(
                  fontSize: 11,
                  fontWeight: FontWeight.w900,
                  color: const Color(0xFFC48000),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
