import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class ScheduleGeneratingCard extends StatefulWidget {
  const ScheduleGeneratingCard({super.key});

  @override
  State<ScheduleGeneratingCard> createState() => _ScheduleGeneratingCardState();
}

class _ScheduleGeneratingCardState extends State<ScheduleGeneratingCard>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;
  late final Animation<double> _pulse;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1800),
    )..repeat(reverse: true);
    _pulse = Tween<double>(begin: 0.4, end: 1.0).animate(
      CurvedAnimation(parent: _controller, curve: Curves.easeInOut),
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.fromLTRB(16, 24, 16, 0),
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 24),
      decoration: BoxDecoration(
        color: const Color(0xFFF0EEFF),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Column(
        children: [
          AnimatedBuilder(
            animation: _pulse,
            builder: (context, child) => Opacity(
              opacity: _pulse.value,
              child: child,
            ),
            child: _SpinningSparkle(size: 36),
          ),
          const SizedBox(height: 16),
          Text(
            'Getting your day ready',
            style: GoogleFonts.nunito(
              fontSize: 16,
              fontWeight: FontWeight.w800,
              color: const Color(0xFF6355EE),
            ),
          ),
          const SizedBox(height: 6),
          Text(
            'Rail is putting together your schedule.\nIt\'ll be ready in just a moment.',
            textAlign: TextAlign.center,
            style: GoogleFonts.nunito(
              fontSize: 13,
              fontWeight: FontWeight.w600,
              color: const Color(0xFF6355EE).withValues(alpha: 0.65),
              height: 1.5,
            ),
          ),
        ],
      ),
    );
  }
}

class _SpinningSparkle extends StatefulWidget {
  final double size;
  const _SpinningSparkle({required this.size});

  @override
  State<_SpinningSparkle> createState() => _SpinningSparkleState();
}

class _SpinningSparkleState extends State<_SpinningSparkle>
    with SingleTickerProviderStateMixin {
  late final AnimationController _spin;

  @override
  void initState() {
    super.initState();
    _spin = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 4),
    )..repeat();
  }

  @override
  void dispose() {
    _spin.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _spin,
      builder: (context, _) => Transform.rotate(
        angle: _spin.value * 2 * math.pi,
        child: Text(
          '✦',
          style: TextStyle(
            fontSize: widget.size,
            color: const Color(0xFF6355EE),
          ),
        ),
      ),
    );
  }
}
