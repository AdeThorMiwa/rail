import 'package:flutter/material.dart';

class DecoCircle extends StatelessWidget {
  final Color color;
  final double size;

  const DecoCircle({super.key, required this.color, required this.size});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.14),
        shape: BoxShape.circle,
      ),
    );
  }
}
