import 'dart:math';
import 'package:flutter/material.dart';

enum AnimChipType { float, wiggle, spin }

class AnimatedChip extends StatefulWidget {
  final String emoji;
  final List<Color> colors;
  final Color shadowColor;
  final AnimChipType animType;
  final double size;

  const AnimatedChip({
    super.key,
    required this.emoji,
    required this.colors,
    required this.shadowColor,
    required this.animType,
    this.size = 108,
  });

  @override
  State<AnimatedChip> createState() => _AnimatedChipState();
}

class _AnimatedChipState extends State<AnimatedChip>
    with SingleTickerProviderStateMixin {
  late final AnimationController _ctrl;
  late final Animation<double> _anim;

  @override
  void initState() {
    super.initState();
    switch (widget.animType) {
      case AnimChipType.float:
        _ctrl = AnimationController(
          duration: const Duration(milliseconds: 3200),
          vsync: this,
        )..repeat(reverse: true);
        _anim = Tween<double>(begin: 0, end: -10).animate(
          CurvedAnimation(parent: _ctrl, curve: Curves.easeInOut),
        );
      case AnimChipType.wiggle:
        _ctrl = AnimationController(
          duration: const Duration(milliseconds: 2800),
          vsync: this,
        )..repeat(reverse: true);
        _anim = Tween<double>(begin: -0.18, end: 0.18).animate(
          CurvedAnimation(parent: _ctrl, curve: Curves.easeInOut),
        );
      case AnimChipType.spin:
        _ctrl = AnimationController(
          duration: const Duration(seconds: 6),
          vsync: this,
        )..repeat();
        _anim = Tween<double>(begin: 0, end: 2 * pi).animate(_ctrl);
    }
  }

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final s = widget.size;

    return AnimatedBuilder(
      animation: _anim,
      builder: (_, child) {
        return switch (widget.animType) {
          AnimChipType.float => Transform.translate(
              offset: Offset(0, _anim.value),
              child: child,
            ),
          AnimChipType.wiggle || AnimChipType.spin => Transform.rotate(
              angle: _anim.value,
              child: child,
            ),
        };
      },
      child: Container(
        width: s,
        height: s,
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: widget.colors,
          ),
          borderRadius: BorderRadius.circular(s * 0.333),
          boxShadow: [
            BoxShadow(
              color: widget.shadowColor.withValues(alpha: 0.45),
              blurRadius: 40,
              offset: const Offset(0, 16),
            ),
          ],
        ),
        child: Center(
          child: Text(
            widget.emoji,
            style: TextStyle(fontSize: s * 0.48),
          ),
        ),
      ),
    );
  }
}
