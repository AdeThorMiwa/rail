import 'package:flutter/material.dart';

class ConnieFab extends StatelessWidget {
  final VoidCallback onTap;

  const ConnieFab({super.key, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        width: 54,
        height: 54,
        decoration: BoxDecoration(
          gradient: const LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [Color(0xFF6355EE), Color(0xFF9B8FFF)],
          ),
          shape: BoxShape.circle,
          boxShadow: [
            BoxShadow(
              color: const Color(0xFF6355EE).withValues(alpha: 0.44),
              blurRadius: 24,
              offset: const Offset(0, 8),
            ),
          ],
        ),
        child: const Center(child: Text('🎩', style: TextStyle(fontSize: 22))),
      ),
    );
  }
}
