import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../../core/widgets/deco_circle.dart';
import 'animated_chip.dart';
import 'dots_indicator.dart';
import 'onboarding_gradient_button.dart';
import '../screens/onboarding_screen.dart';

class MobileOnboardingPage extends StatelessWidget {
  final OnboardingPage page;
  final int index;
  final int pageCount;
  final int currentIndex;
  final VoidCallback onNext;
  final VoidCallback onSkip;

  const MobileOnboardingPage({
    super.key,
    required this.page,
    required this.index,
    required this.pageCount,
    required this.currentIndex,
    required this.onNext,
    required this.onSkip,
  });

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        Positioned(
          top: -80,
          right: -70,
          child: DecoCircle(color: page.deco1, size: 220),
        ),
        Positioned(
          bottom: 120,
          left: -30,
          child: DecoCircle(color: page.deco2, size: 100),
        ),
        SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(28, 8, 28, 32),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Align(
                  alignment: Alignment.centerRight,
                  child: index < pageCount - 1
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
                const SizedBox(height: 16),
                Center(
                  child: AnimatedChip(
                    emoji: page.emoji,
                    colors: page.chipColors,
                    shadowColor: page.chipShadow,
                    animType: page.animType,
                  ),
                ),
                const SizedBox(height: 36),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text.rich(
                        TextSpan(
                          children: [
                            TextSpan(
                              text: page.title,
                              style: GoogleFonts.nunito(
                                fontSize: 30,
                                fontWeight: FontWeight.w900,
                                color: const Color(0xFF1A1A2E),
                                height: 1.25,
                              ),
                            ),
                            TextSpan(
                              text: page.titleHighlight,
                              style: GoogleFonts.nunito(
                                fontSize: 30,
                                fontWeight: FontWeight.w900,
                                color: page.highlightColor,
                                height: 1.25,
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 16),
                      Text(
                        page.subtitle,
                        style: GoogleFonts.nunito(
                          fontSize: 16,
                          fontWeight: FontWeight.w600,
                          color: const Color(0xFF7878A0),
                          height: 1.75,
                        ),
                      ),
                    ],
                  ),
                ),
                DotsIndicator(
                  count: pageCount,
                  current: currentIndex,
                  activeColor: page.dotColor,
                ),
                const SizedBox(height: 20),
                OnboardingGradientButton(
                  label: index < pageCount - 1 ? 'Next' : 'Get started 🎉',
                  trailingIcon: index < pageCount - 1 ? Icons.chevron_right_rounded : null,
                  colors: page.buttonColors,
                  shadowColor: page.buttonShadow,
                  onTap: onNext,
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}
