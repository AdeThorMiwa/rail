import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import '../data/models/cycle_models.dart';

class CycleChatButton extends StatelessWidget {
  final UserCycle cycle;

  const CycleChatButton({super.key, required this.cycle});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () => context.push(
        '/chat/CYCLE/${cycle.pid}?title=${Uri.encodeComponent(cycle.title)}',
      ),
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.symmetric(vertical: 14),
        decoration: BoxDecoration(
          gradient: const LinearGradient(
            colors: [Color(0xFF7B6EFF), Color(0xFFB57BFF)],
          ),
          borderRadius: BorderRadius.circular(14),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Text('🎩', style: TextStyle(fontSize: 16)),
            const SizedBox(width: 8),
            Text(
              'Chat with Connie about this cycle',
              style: GoogleFonts.nunito(
                fontSize: 14,
                fontWeight: FontWeight.w900,
                color: Colors.white,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
