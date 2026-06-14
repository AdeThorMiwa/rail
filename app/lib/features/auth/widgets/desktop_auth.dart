import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../../core/widgets/deco_circle.dart';

class DesktopAuthShell extends StatelessWidget {
  final Widget form;

  const DesktopAuthShell({super.key, required this.form});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Row(
        children: [
          Expanded(
            flex: 5,
            child: _BrandingPanel(),
          ),
          Expanded(
            flex: 6,
            child: Container(
              color: const Color(0xFFF4F8FF),
              child: Center(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 56,
                    vertical: 48,
                  ),
                  child: ConstrainedBox(
                    constraints: const BoxConstraints(maxWidth: 420),
                    child: form,
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _BrandingPanel extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFFDEEEFF), Color(0xFFEAE6FF), Color(0xFFD4F0EC)],
        ),
      ),
      child: Stack(
        children: [
          Positioned(
            top: -80,
            right: -70,
            child: DecoCircle(color: const Color(0xFFA0C4FF), size: 300),
          ),
          Positioned(
            bottom: 60,
            left: -60,
            child: DecoCircle(color: const Color(0xFFC77DFF), size: 220),
          ),
          Positioned(
            top: 120,
            left: 30,
            child: DecoCircle(color: const Color(0xFF80CBC4), size: 100),
          ),
          Positioned(
            bottom: 200,
            right: 20,
            child: DecoCircle(color: const Color(0xFFBDB2FF), size: 70),
          ),
          SafeArea(
            child: Padding(
              padding: const EdgeInsets.all(48),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '🚂 Rail',
                    style: GoogleFonts.nunito(
                      fontSize: 28,
                      fontWeight: FontWeight.w900,
                      color: const Color(0xFF1A1A2E).withValues(alpha: 0.8),
                      letterSpacing: -0.5,
                    ),
                  ),
                  const Spacer(),
                  _Tagline(),
                  const Spacer(flex: 2),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _Tagline extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Your personal\nconsistency engine.',
          style: GoogleFonts.nunito(
            fontSize: 36,
            fontWeight: FontWeight.w900,
            color: const Color(0xFF1A1A2E),
            height: 1.2,
          ),
        ),
        const SizedBox(height: 16),
        Text(
          'Build the habits and finish the projects\nthat actually matter to you.',
          style: GoogleFonts.nunito(
            fontSize: 16,
            fontWeight: FontWeight.w600,
            color: const Color(0xFF7878A0),
            height: 1.7,
          ),
        ),
        const SizedBox(height: 32),
        _PillBadge(label: '⚡ Execute daily'),
        const SizedBox(height: 10),
        _PillBadge(label: '🌱 Build real habits'),
        const SizedBox(height: 10),
        _PillBadge(label: '🌟 Weekly rituals'),
      ],
    );
  }
}

class _PillBadge extends StatelessWidget {
  final String label;

  const _PillBadge({required this.label});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.6),
        borderRadius: BorderRadius.circular(24),
        border: Border.all(
          color: Colors.white.withValues(alpha: 0.8),
          width: 1.5,
        ),
      ),
      child: Text(
        label,
        style: GoogleFonts.nunito(
          fontSize: 14,
          fontWeight: FontWeight.w700,
          color: const Color(0xFF4A4A6A),
        ),
      ),
    );
  }
}
