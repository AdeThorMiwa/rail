import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import '../providers/cycle_provider.dart';
import '../widgets/create_cycle_form.dart';
import '../widgets/cycle_chat_button.dart';
import '../widgets/cycle_focus_section.dart';
import '../widgets/cycle_hero_card.dart';

class CycleScreen extends ConsumerWidget {
  const CycleScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final cycleState = ref.watch(cycleProvider);

    return Scaffold(
      backgroundColor: const Color(0xFFF4F8FF),
      body: cycleState.when(
        loading: () => const Center(
          child: CircularProgressIndicator(color: Color(0xFF7B6EFF)),
        ),
        error: (_, _) => Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                'Could not load cycle',
                style: GoogleFonts.nunito(
                  fontWeight: FontWeight.w700,
                  color: const Color(0xFF9090AA),
                ),
              ),
              const SizedBox(height: 12),
              TextButton(
                onPressed: () => ref.invalidate(cycleProvider),
                child: Text(
                  'Retry',
                  style: GoogleFonts.nunito(
                    fontWeight: FontWeight.w800,
                    color: const Color(0xFF7B6EFF),
                  ),
                ),
              ),
            ],
          ),
        ),
        data: (cycle) => CustomScrollView(
          slivers: [
            SliverAppBar(
              backgroundColor: const Color(0xFFF4F8FF),
              elevation: 0,
              pinned: true,
              leading: IconButton(
                icon: const Icon(Icons.arrow_back_ios_new_rounded, size: 18),
                color: const Color(0xFF1A1A2E),
                onPressed: () => Navigator.of(context).pop(),
              ),
              title: Text(
                cycle == null ? 'New Cycle' : 'My Cycle',
                style: GoogleFonts.nunito(
                  fontSize: 18,
                  fontWeight: FontWeight.w900,
                  color: const Color(0xFF1A1A2E),
                ),
              ),
            ),
            if (cycle == null)
              const SliverToBoxAdapter(child: CreateCycleForm())
            else
              SliverToBoxAdapter(
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(20, 0, 20, 32),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      CycleHeroCard(cycle: cycle),
                      const SizedBox(height: 20),
                      CycleFocusSection(cycle: cycle),
                      const SizedBox(height: 20),
                      CycleChatButton(cycle: cycle),
                    ],
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}
