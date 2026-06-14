import 'package:flutter/material.dart';

class MilestonePulsingNode extends StatefulWidget {
  const MilestonePulsingNode({super.key});

  @override
  State<MilestonePulsingNode> createState() => _MilestonePulsingNodeState();
}

class _MilestonePulsingNodeState extends State<MilestonePulsingNode>
    with SingleTickerProviderStateMixin {
  late final AnimationController _ctrl;
  late final Animation<double> _spread;

  @override
  void initState() {
    super.initState();
    _ctrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 2200),
    )..repeat(reverse: true);
    _spread = Tween<double>(begin: 4.0, end: 8.0).animate(
      CurvedAnimation(parent: _ctrl, curve: Curves.easeInOut),
    );
  }

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _spread,
      builder: (_, _) => Container(
        width: 18,
        height: 18,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          gradient: const LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [Color(0xFF6355EE), Color(0xFF9B8FFF)],
          ),
          boxShadow: [
            BoxShadow(
              color: const Color(0xFF7B6EFF).withValues(alpha: 0.15),
              spreadRadius: _spread.value,
              blurRadius: 0,
            ),
            const BoxShadow(
              color: Color(0xFF6355EE),
              blurRadius: 12,
              offset: Offset(0, 3),
            ),
          ],
        ),
      ),
    );
  }
}
