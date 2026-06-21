import 'dart:async';

import 'package:alarm/alarm.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:vibration/vibration.dart';

class AlarmScreen extends StatefulWidget {
  final AlarmSettings alarmSettings;
  final int ringSeconds;
  final VoidCallback onDismiss;

  const AlarmScreen({
    super.key,
    required this.alarmSettings,
    required this.ringSeconds,
    required this.onDismiss,
  });

  @override
  State<AlarmScreen> createState() => _AlarmScreenState();
}

class _AlarmScreenState extends State<AlarmScreen>
    with TickerProviderStateMixin {
  late AnimationController _pulse1;
  late AnimationController _pulse2;
  late AnimationController _pulse3;

  late int _secondsLeft;
  Timer? _countdown;

  @override
  void initState() {
    super.initState();
    _secondsLeft = widget.ringSeconds;

    _pulse1 = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1800),
    )..repeat();

    _pulse2 = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1800),
    );
    _pulse3 = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1800),
    );

    Future.delayed(const Duration(milliseconds: 450), () {
      if (mounted) _pulse2.repeat();
    });
    Future.delayed(const Duration(milliseconds: 900), () {
      if (mounted) _pulse3.repeat();
    });

    _startCountdown();
    _startVibration();
  }

  void _startCountdown() {
    _countdown = Timer.periodic(const Duration(seconds: 1), (_) {
      if (!mounted) return;
      setState(() => _secondsLeft--);
      if (_secondsLeft <= 0) _autoDismiss();
    });
  }

  void _startVibration() async {
    final hasVibrator = await Vibration.hasVibrator();
    if (!hasVibrator || !mounted) return;
    // Alarm-style: short buzz, pause, short buzz, pause, long buzz, long pause — repeat
    Vibration.vibrate(pattern: [0, 300, 150, 300, 150, 600, 900], repeat: 0);
  }

  void _autoDismiss() {
    Vibration.cancel();
    widget.onDismiss();
  }

  void _handleDismiss() {
    _countdown?.cancel();
    Vibration.cancel();
    widget.onDismiss();
  }

  @override
  void dispose() {
    _countdown?.cancel();
    _pulse1.dispose();
    _pulse2.dispose();
    _pulse3.dispose();
    super.dispose();
  }

  String get _countdownLabel {
    final m = _secondsLeft ~/ 60;
    final s = _secondsLeft % 60;
    return '$m:${s.toString().padLeft(2, '0')}';
  }

  bool get _isWakeAlarm => widget.alarmSettings.id == 1000;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            colors: [Color(0xFF6B5CE7), Color(0xFF9B8AFB)],
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
          ),
        ),
        child: SafeArea(
          child: Column(
            children: [
              const Spacer(),
              _buildPulseRings(),
              const SizedBox(height: 40),
              _buildLabel(),
              const Spacer(),
              _buildDismissButton(),
              const SizedBox(height: 48),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildPulseRings() {
    return SizedBox(
      width: 240,
      height: 240,
      child: Stack(
        alignment: Alignment.center,
        children: [
          _PulseRing(
            controller: _pulse1,
            baseColor: Colors.white.withValues(alpha: 0.08),
            maxScale: 2.4,
          ),
          _PulseRing(
            controller: _pulse2,
            baseColor: Colors.white.withValues(alpha: 0.12),
            maxScale: 1.9,
          ),
          _PulseRing(
            controller: _pulse3,
            baseColor: Colors.white.withValues(alpha: 0.18),
            maxScale: 1.5,
          ),
          Container(
            width: 100,
            height: 100,
            decoration: BoxDecoration(
              color: Colors.white.withValues(alpha: 0.25),
              shape: BoxShape.circle,
            ),
            child: Icon(
              _isWakeAlarm ? Icons.wb_sunny_rounded : Icons.play_arrow_rounded,
              color: Colors.white,
              size: 48,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildLabel() {
    final title = widget.alarmSettings.notificationSettings.body;
    final eyebrow = _isWakeAlarm ? 'GOOD MORNING' : 'STARTING NOW';

    return Column(
      children: [
        Text(
          eyebrow,
          style: GoogleFonts.nunito(
            fontSize: 12,
            fontWeight: FontWeight.w900,
            color: Colors.white.withValues(alpha: 0.7),
            letterSpacing: 2.0,
          ),
        ),
        const SizedBox(height: 10),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 32),
          child: Text(
            title,
            textAlign: TextAlign.center,
            style: GoogleFonts.nunito(
              fontSize: 26,
              fontWeight: FontWeight.w900,
              color: Colors.white,
              height: 1.2,
            ),
          ),
        ),
        const SizedBox(height: 16),
        Text(
          'Auto-dismissing in $_countdownLabel',
          style: GoogleFonts.nunito(
            fontSize: 13,
            fontWeight: FontWeight.w600,
            color: Colors.white.withValues(alpha: 0.6),
          ),
        ),
      ],
    );
  }

  Widget _buildDismissButton() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 48),
      child: GestureDetector(
        onTap: _handleDismiss,
        child: Container(
          width: double.infinity,
          padding: const EdgeInsets.symmetric(vertical: 18),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(20),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withValues(alpha: 0.15),
                blurRadius: 20,
                offset: const Offset(0, 8),
              ),
            ],
          ),
          child: Text(
            'Dismiss',
            textAlign: TextAlign.center,
            style: GoogleFonts.nunito(
              fontSize: 17,
              fontWeight: FontWeight.w900,
              color: const Color(0xFF6B5CE7),
            ),
          ),
        ),
      ),
    );
  }
}

class _PulseRing extends StatelessWidget {
  final AnimationController controller;
  final Color baseColor;
  final double maxScale;

  const _PulseRing({
    required this.controller,
    required this.baseColor,
    required this.maxScale,
  });

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: controller,
      builder: (_, child) {
        final t = Curves.easeOut.transform(controller.value);
        final scale = 1.0 + (maxScale - 1.0) * t;
        final opacity = 1.0 - t;
        return Transform.scale(
          scale: scale,
          child: Opacity(
            opacity: opacity,
            child: child,
          ),
        );
      },
      child: Container(
        width: 100,
        height: 100,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          color: baseColor,
        ),
      ),
    );
  }
}
