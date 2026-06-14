import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../data/models/scheduling_models.dart';

class EnergyRow extends StatelessWidget {
  final EnergyPattern selected;
  final ValueChanged<EnergyPattern> onChanged;

  const EnergyRow({super.key, required this.selected, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: EnergyPattern.values.map((e) {
        final isSelected = e == selected;
        final isFirst = e == EnergyPattern.values.first;
        final isLast = e == EnergyPattern.values.last;
        return Expanded(
          child: Padding(
            padding: EdgeInsets.only(left: isFirst ? 0 : 4, right: isLast ? 0 : 4),
            child: GestureDetector(
              onTap: () => onChanged(e),
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 220),
                curve: Curves.easeOut,
                padding: const EdgeInsets.symmetric(vertical: 14, horizontal: 6),
                decoration: BoxDecoration(
                  color: isSelected ? const Color(0xFFF0ECFF) : Colors.white,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(
                    color: isSelected ? const Color(0xFF7B6EFF) : const Color(0xFFE8E2FF),
                    width: 2,
                  ),
                  boxShadow: isSelected
                      ? [
                          BoxShadow(
                            color: const Color(0xFF7B6EFF).withValues(alpha: 0.2),
                            blurRadius: 12,
                            offset: const Offset(0, 4),
                          ),
                        ]
                      : null,
                ),
                child: Column(
                  children: [
                    Text(e.emoji, style: const TextStyle(fontSize: 22)),
                    const SizedBox(height: 5),
                    Text(
                      e.label,
                      style: GoogleFonts.nunito(
                        fontSize: 12,
                        fontWeight: FontWeight.w900,
                        color: isSelected ? const Color(0xFF7B6EFF) : const Color(0xFF1A1A2E),
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      e.description,
                      textAlign: TextAlign.center,
                      style: GoogleFonts.nunito(
                        fontSize: 10,
                        fontWeight: FontWeight.w700,
                        color: const Color(0xFF9090AA),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        );
      }).toList(),
    );
  }
}
