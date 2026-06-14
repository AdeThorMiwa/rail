import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class OffHoursScreen extends StatelessWidget {
  final VoidCallback onConnieTapped;

  const OffHoursScreen({super.key, required this.onConnieTapped});

  @override
  Widget build(BuildContext context) {
    final hour = DateTime.now().hour;
    final isPreDawn = hour < 7;

    return Container(
      constraints: const BoxConstraints.expand(),
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFF4C1D95)],
        ),
      ),
      child: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 32),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Spacer(flex: 2),
              _MoonIllustration(),
              const SizedBox(height: 40),
              Text(
                isPreDawn ? 'Still sleeping 🌙' : 'Building your day ✨',
                style: GoogleFonts.nunito(
                  fontSize: 26,
                  fontWeight: FontWeight.w900,
                  color: Colors.white,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 14),
              Text(
                isPreDawn
                    ? "Connie is resting too.\nYour schedule will be ready at 7:00 AM."
                    : "Connie is putting together your schedule.\nCheck back in a moment.",
                style: GoogleFonts.nunito(
                  fontSize: 15,
                  fontWeight: FontWeight.w600,
                  color: Colors.white.withValues(alpha: 0.65),
                  height: 1.65,
                ),
                textAlign: TextAlign.center,
              ),
              const Spacer(flex: 2),
              _ChatButton(onTap: onConnieTapped),
              const SizedBox(height: 48),
            ],
          ),
        ),
      ),
    );
  }
}

class _MoonIllustration extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Stack(
      alignment: Alignment.center,
      children: [
        // Outer glow
        Container(
          width: 160,
          height: 160,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            gradient: RadialGradient(
              colors: [
                const Color(0xFF818CF8).withValues(alpha: 0.25),
                const Color(0xFF818CF8).withValues(alpha: 0.0),
              ],
            ),
          ),
        ),
        // Moon circle
        Container(
          width: 96,
          height: 96,
          decoration: const BoxDecoration(
            shape: BoxShape.circle,
            gradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [Color(0xFFC7D2FE), Color(0xFFA5B4FC)],
            ),
            boxShadow: [
              BoxShadow(
                color: Color(0x44818CF8),
                blurRadius: 32,
                offset: Offset(0, 8),
              ),
            ],
          ),
          child: const Icon(
            Icons.nightlight_round,
            size: 52,
            color: Color(0xFF312E81),
          ),
        ),
        // Stars
        Positioned(top: 12, right: 20, child: _Star(size: 8)),
        Positioned(top: 28, right: 8, child: _Star(size: 5)),
        Positioned(bottom: 18, left: 14, child: _Star(size: 6)),
        Positioned(top: 8, left: 30, child: _Star(size: 4)),
      ],
    );
  }
}

class _Star extends StatelessWidget {
  final double size;
  const _Star({required this.size});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: Colors.white.withValues(alpha: 0.7),
        boxShadow: [
          BoxShadow(
            color: Colors.white.withValues(alpha: 0.4),
            blurRadius: size,
          ),
        ],
      ),
    );
  }
}

class _ChatButton extends StatelessWidget {
  final VoidCallback onTap;
  const _ChatButton({required this.onTap});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 16),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(50),
          gradient: const LinearGradient(
            colors: [Color(0xFF818CF8), Color(0xFFA78BFA)],
          ),
          boxShadow: [
            BoxShadow(
              color: const Color(0xFF818CF8).withValues(alpha: 0.45),
              blurRadius: 20,
              offset: const Offset(0, 8),
            ),
          ],
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(
              Icons.chat_bubble_outline_rounded,
              color: Colors.white,
              size: 18,
            ),
            const SizedBox(width: 10),
            Text(
              'Chat with Connie',
              style: GoogleFonts.nunito(
                fontSize: 15,
                fontWeight: FontWeight.w800,
                color: Colors.white,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
