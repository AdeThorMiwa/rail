import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class ConnieHeader extends StatelessWidget {
  final VoidCallback onBack;
  final VoidCallback onMenu;
  final String subtitle;

  const ConnieHeader({
    super.key,
    required this.onBack,
    required this.onMenu,
    this.subtitle = "Rail's Conductor",
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.fromLTRB(
          16, MediaQuery.of(context).padding.top + 12, 16, 14),
      decoration: const BoxDecoration(
        color: Colors.white,
        border: Border(bottom: BorderSide(color: Color(0xFFF0ECFF), width: 1.5)),
      ),
      child: Row(
        children: [
          GestureDetector(
            onTap: onBack,
            child: const Icon(
              Icons.chevron_left_rounded,
              size: 22,
              color: Color(0xFF7B6EFF),
            ),
          ),
          const SizedBox(width: 12),
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              gradient: const LinearGradient(
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: [Color(0xFF6355EE), Color(0xFF9B8FFF)],
              ),
              boxShadow: [
                BoxShadow(
                  color: const Color(0xFF6355EE).withValues(alpha: 0.3),
                  blurRadius: 10,
                  offset: const Offset(0, 3),
                ),
              ],
            ),
            child: const Center(child: Text('🎩', style: TextStyle(fontSize: 19))),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text('Connie',
                    style: GoogleFonts.nunito(
                        fontSize: 15,
                        fontWeight: FontWeight.w900,
                        color: const Color(0xFF1A1A2E))),
                Text(subtitle,
                    style: GoogleFonts.nunito(
                        fontSize: 11,
                        fontWeight: FontWeight.w700,
                        color: const Color(0xFF9B8FFF))),
              ],
            ),
          ),
          GestureDetector(
            onTap: onMenu,
            child: const Icon(
              Icons.grid_view_rounded,
              size: 22,
              color: Color(0xFFB0AACC),
            ),
          ),
        ],
      ),
    );
  }
}
