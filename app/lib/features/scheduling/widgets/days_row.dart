import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../data/models/scheduling_models.dart';

class DaysRow extends StatelessWidget {
  final Set<int> selected;
  final ValueChanged<int> onToggle;

  const DaysRow({super.key, required this.selected, required this.onToggle});

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: List.generate(7, (i) {
        final weekday = i + 1;
        final day = SchedulingDayModel(weekday);
        final isSelected = selected.contains(weekday);
        return GestureDetector(
          onTap: () => onToggle(weekday),
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 220),
            curve: Curves.easeOut,
            width: 38,
            height: 38,
            decoration: BoxDecoration(
              gradient: isSelected
                  ? const LinearGradient(
                      colors: [Color(0xFF7B6EFF), Color(0xFF9B8FFF)],
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                    )
                  : null,
              color: isSelected ? null : Colors.white,
              shape: BoxShape.circle,
              border: Border.all(
                color: isSelected ? Colors.transparent : const Color(0xFFE8E2FF),
                width: 2,
              ),
              boxShadow: isSelected
                  ? [
                      BoxShadow(
                        color: const Color(0xFF7B6EFF).withValues(alpha: 0.3),
                        blurRadius: 8,
                        offset: const Offset(0, 3),
                      ),
                    ]
                  : null,
            ),
            child: Center(
              child: Text(
                day.label,
                style: GoogleFonts.nunito(
                  fontSize: 12,
                  fontWeight: FontWeight.w900,
                  color: isSelected ? Colors.white : const Color(0xFFAAAAC0),
                ),
              ),
            ),
          ),
        );
      }),
    );
  }
}
