import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../../core/widgets/deco_circle.dart';
import 'animated_chip.dart';
import 'dots_indicator.dart';
import 'onboarding_gradient_button.dart';
import '../screens/onboarding_screen.dart';

class DesktopOnboarding extends StatelessWidget {
  final List<OnboardingPage> pages;
  final int currentPage;
  final VoidCallback onNext;
  final VoidCallback onSkip;

  const DesktopOnboarding({
    super.key,
    required this.pages,
    required this.currentPage,
    required this.onNext,
    required this.onSkip,
  });

  @override
  Widget build(BuildContext context) {
    final page = pages[currentPage];

    return Scaffold(
      backgroundColor: const Color(0xFFF4F8FF),
      body: Row(
        children: [
          Expanded(
            flex: 5,
            child: AnimatedSwitcher(
              duration: const Duration(milliseconds: 500),
              switchInCurve: Curves.easeOut,
              switchOutCurve: Curves.easeIn,
              child: _LeftPanel(
                key: ValueKey(currentPage),
                page: page,
              ),
            ),
          ),
          Expanded(
            flex: 6,
            child: Container(
              color: const Color(0xFFF4F8FF),
              child: SafeArea(
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(56, 32, 64, 48),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Align(
                        alignment: Alignment.centerRight,
                        child: currentPage < pages.length - 1
                            ? TextButton(
                                onPressed: onSkip,
                                child: Text(
                                  'Skip',
                                  style: GoogleFonts.nunito(
                                    fontSize: 15,
                                    fontWeight: FontWeight.w700,
                                    color: const Color(0xFF9090AA),
                                  ),
                                ),
                              )
                            : const SizedBox(height: 40),
                      ),
                      const Spacer(),
                      AnimatedSwitcher(
                        duration: const Duration(milliseconds: 380),
                        switchInCurve: Curves.easeOut,
                        switchOutCurve: Curves.easeIn,
                        transitionBuilder: (child, anim) => FadeTransition(
                          opacity: anim,
                          child: SlideTransition(
                            position: Tween<Offset>(
                              begin: const Offset(0.04, 0),
                              end: Offset.zero,
                            ).animate(anim),
                            child: child,
                          ),
                        ),
                        child: _RightContent(
                          key: ValueKey(currentPage),
                          page: page,
                        ),
                      ),
                      const SizedBox(height: 40),
                      DotsIndicator(
                        count: pages.length,
                        current: currentPage,
                        activeColor: page.dotColor,
                      ),
                      const SizedBox(height: 28),
                      SizedBox(
                        width: 320,
                        child: OnboardingGradientButton(
                          label: currentPage < pages.length - 1
                              ? 'Next'
                              : 'Get started 🎉',
                          trailingIcon: currentPage < pages.length - 1
                              ? Icons.chevron_right_rounded
                              : null,
                          colors: page.buttonColors,
                          shadowColor: page.buttonShadow,
                          onTap: onNext,
                        ),
                      ),
                      const Spacer(),
                    ],
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

class _LeftPanel extends StatelessWidget {
  final OnboardingPage page;

  const _LeftPanel({super.key, required this.page});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: page.panelGradient,
        ),
      ),
      child: Stack(
        children: [
          Positioned(
            top: -80,
            right: -70,
            child: DecoCircle(color: page.deco1, size: 260),
          ),
          Positioned(
            bottom: 80,
            left: -50,
            child: DecoCircle(color: page.deco2, size: 180),
          ),
          Positioned(
            top: 60,
            left: 40,
            child: DecoCircle(color: page.chipColors[0], size: 80),
          ),
          SafeArea(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const SizedBox(height: 40),
                Text(
                  '🚂 Rail',
                  style: GoogleFonts.nunito(
                    fontSize: 22,
                    fontWeight: FontWeight.w900,
                    color: const Color(0xFF1A1A2E).withValues(alpha: 0.75),
                    letterSpacing: -0.3,
                  ),
                ),
                const Spacer(),
                AnimatedChip(
                  emoji: page.emoji,
                  colors: page.chipColors,
                  shadowColor: page.chipShadow,
                  animType: page.animType,
                  size: 140,
                ),
                const SizedBox(height: 28),
                Text(
                  _tagline(page),
                  style: GoogleFonts.nunito(
                    fontSize: 15,
                    fontWeight: FontWeight.w700,
                    color: page.highlightColor.withValues(alpha: 0.8),
                  ),
                ),
                const Spacer(flex: 2),
              ],
            ),
          ),
        ],
      ),
    );
  }

  String _tagline(OnboardingPage p) {
    if (p.emoji == '⚡') return 'Focus on execution';
    if (p.emoji == '🌱') return 'Goals that stick';
    if (p.emoji == '🃏') return 'One task at a time';
    return 'Weekly review ritual';
  }
}

class _RightContent extends StatelessWidget {
  final OnboardingPage page;

  const _RightContent({super.key, required this.page});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        Text.rich(
          TextSpan(
            children: [
              TextSpan(
                text: page.title,
                style: GoogleFonts.nunito(
                  fontSize: 38,
                  fontWeight: FontWeight.w900,
                  color: const Color(0xFF1A1A2E),
                  height: 1.2,
                ),
              ),
              TextSpan(
                text: page.titleHighlight,
                style: GoogleFonts.nunito(
                  fontSize: 38,
                  fontWeight: FontWeight.w900,
                  color: page.highlightColor,
                  height: 1.2,
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 20),
        Text(
          page.subtitle,
          style: GoogleFonts.nunito(
            fontSize: 17,
            fontWeight: FontWeight.w600,
            color: const Color(0xFF7878A0),
            height: 1.8,
          ),
        ),
      ],
    );
  }
}
