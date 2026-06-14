import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../providers/intro_seen_provider.dart';
import '../widgets/animated_chip.dart';
import '../widgets/desktop_onboarding.dart';
import '../widgets/mobile_onboarding_page.dart';

class OnboardingPage {
  final String emoji;
  final List<Color> chipColors;
  final Color chipShadow;
  final String title;
  final String titleHighlight;
  final Color highlightColor;
  final String subtitle;
  final Color dotColor;
  final List<Color> buttonColors;
  final Color buttonShadow;
  final Color deco1;
  final Color deco2;
  final AnimChipType animType;
  final List<Color> panelGradient;

  const OnboardingPage({
    required this.emoji,
    required this.chipColors,
    required this.chipShadow,
    required this.title,
    required this.titleHighlight,
    required this.highlightColor,
    required this.subtitle,
    required this.dotColor,
    required this.buttonColors,
    required this.buttonShadow,
    required this.deco1,
    required this.deco2,
    required this.animType,
    required this.panelGradient,
  });
}

const kOnboardingPages = [
  OnboardingPage(
    emoji: '⚡',
    chipColors: [Color(0xFFC2D8FF), Color(0xFF7BB3FF)],
    chipShadow: Color(0xFF4D9FFF),
    title: 'Most apps make you plan.\n',
    titleHighlight: 'Rail makes you execute.',
    highlightColor: Color(0xFF4D9FFF),
    subtitle:
        'You tell Rail what matters. Rail figures out when. You just show up and do the thing. ✨',
    dotColor: Color(0xFF7BB3FF),
    buttonColors: [Color(0xFF7BB3FF), Color(0xFF4D9FFF)],
    buttonShadow: Color(0xFF4D9FFF),
    deco1: Color(0xFFA0C4FF),
    deco2: Color(0xFFBDB2FF),
    animType: AnimChipType.float,
    panelGradient: [Color(0xFFDEEEFF), Color(0xFFEAE6FF), Color(0xFFD4EEFF)],
  ),
  OnboardingPage(
    emoji: '🌱',
    chipColors: [Color(0xFFA8E6DF), Color(0xFF5BBFB8)],
    chipShadow: Color(0xFF3AAFA9),
    title: 'Define your goals once.\n',
    titleHighlight: 'Rail owns the schedule.',
    highlightColor: Color(0xFF3AAFA9),
    subtitle:
        'Habits that go forever. Projects that finish. Rail knows the difference — and plans around your life. 🎯',
    dotColor: Color(0xFF5BBFB8),
    buttonColors: [Color(0xFF5BBFB8), Color(0xFF3AAFA9)],
    buttonShadow: Color(0xFF3AAFA9),
    deco1: Color(0xFF80CBC4),
    deco2: Color(0xFFA0C4FF),
    animType: AnimChipType.float,
    panelGradient: [Color(0xFFD4F0EC), Color(0xFFE0F7F4), Color(0xFFD4EEFF)],
  ),
  OnboardingPage(
    emoji: '🃏',
    chipColors: [Color(0xFFD4CEFF), Color(0xFF9B8FFF)],
    chipShadow: Color(0xFF7B6EFF),
    title: 'Every day, a stack.\n',
    titleHighlight: 'Swipe right when done.',
    highlightColor: Color(0xFF7B6EFF),
    subtitle:
        'No calendar. No drag-and-drop. The next thing is always waiting. Just swipe and keep going. 🚀',
    dotColor: Color(0xFF9B8FFF),
    buttonColors: [Color(0xFF9B8FFF), Color(0xFF7B6EFF)],
    buttonShadow: Color(0xFF7B6EFF),
    deco1: Color(0xFFBDB2FF),
    deco2: Color(0xFF80CBC4),
    animType: AnimChipType.wiggle,
    panelGradient: [Color(0xFFE8E4FF), Color(0xFFEEEAFF), Color(0xFFD4F0EC)],
  ),
  OnboardingPage(
    emoji: '🌟',
    chipColors: [Color(0xFFE0AAFF), Color(0xFFC77DFF)],
    chipShadow: Color(0xFFA855F7),
    title: 'Every week, a ritual.\n',
    titleHighlight: 'Rail leads it.',
    highlightColor: Color(0xFFB44FFF),
    subtitle:
        'Reflect on what worked. Carry forward what matters. Plan what\'s next. Rail runs the whole conversation. 💫',
    dotColor: Color(0xFFC77DFF),
    buttonColors: [Color(0xFFC77DFF), Color(0xFFA855F7)],
    buttonShadow: Color(0xFFA855F7),
    deco1: Color(0xFFC77DFF),
    deco2: Color(0xFF7BB3FF),
    animType: AnimChipType.spin,
    panelGradient: [Color(0xFFF0E0FF), Color(0xFFEAD4FF), Color(0xFFD4EEFF)],
  ),
];

class OnboardingScreen extends ConsumerStatefulWidget {
  const OnboardingScreen({super.key});

  @override
  ConsumerState<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends ConsumerState<OnboardingScreen> {
  final _controller = PageController();
  int _page = 0;

  @override
  void initState() {
    super.initState();
    _controller.addListener(() {
      final p = _controller.page?.round() ?? 0;
      if (p != _page) setState(() => _page = p);
    });
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _finish() async {
    await markIntroSeen();
    ref.invalidate(introSeenProvider);
    if (mounted) context.go('/auth');
  }

  void _next() {
    if (_page < kOnboardingPages.length - 1) {
      final isDesktop = MediaQuery.of(context).size.width >= 600;
      if (isDesktop) {
        setState(() => _page++);
      } else {
        _controller.nextPage(
          duration: const Duration(milliseconds: 500),
          curve: Curves.easeInOut,
        );
      }
    } else {
      _finish();
    }
  }

  @override
  Widget build(BuildContext context) {
    final isDesktop = MediaQuery.of(context).size.width >= 600;

    if (isDesktop) {
      return DesktopOnboarding(
        pages: kOnboardingPages,
        currentPage: _page,
        onNext: _next,
        onSkip: _finish,
      );
    }

    return Scaffold(
      backgroundColor: const Color(0xFFF4F8FF),
      body: PageView.builder(
        controller: _controller,
        itemCount: kOnboardingPages.length,
        itemBuilder: (_, i) => MobileOnboardingPage(
          page: kOnboardingPages[i],
          index: i,
          pageCount: kOnboardingPages.length,
          currentIndex: _page,
          onNext: _next,
          onSkip: _finish,
        ),
      ),
    );
  }
}
