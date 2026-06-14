import 'package:flutter/material.dart';

class GoalMonthGrid extends StatelessWidget {
  final List<bool?> days;
  final Color doneColor;
  final Color missColor;

  const GoalMonthGrid({
    super.key,
    required this.days,
    this.doneColor = const Color(0xFF7B6EFF),
    this.missColor = const Color(0xFFFFE0E0),
  });

  @override
  Widget build(BuildContext context) {
    return GridView.count(
      crossAxisCount: 10,
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      mainAxisSpacing: 5,
      crossAxisSpacing: 5,
      children: days.map((d) => _MonthDot(isDone: d, doneColor: doneColor, missColor: missColor)).toList(),
    );
  }
}

class _MonthDot extends StatelessWidget {
  final bool? isDone;
  final Color doneColor;
  final Color missColor;

  const _MonthDot({
    required this.isDone,
    required this.doneColor,
    required this.missColor,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(4),
        gradient: isDone == true
            ? LinearGradient(
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: [doneColor, doneColor.withValues(alpha: 0.7)],
              )
            : null,
        color: isDone == null
            ? const Color(0xFFF0ECFF)
            : isDone == false
                ? missColor
                : null,
      ),
    );
  }
}
