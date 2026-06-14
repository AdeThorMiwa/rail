import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../../core/widgets/connie_fab.dart';
import '../providers/goals_provider.dart';
import '../widgets/empty_intentions.dart';
import '../widgets/goal_card.dart';

class GoalsScreen extends ConsumerWidget {
  const GoalsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(goalsProvider);

    return Scaffold(
      backgroundColor: const Color(0xFFF4F8FF),
      body: SafeArea(
        child: state.when(
          loading: () => const Center(
            child: CircularProgressIndicator(color: Color(0xFF7B6EFF)),
          ),
          error: (_, _) => Center(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  'Something went wrong',
                  style: GoogleFonts.nunito(
                    fontWeight: FontWeight.w700,
                    color: const Color(0xFF9090AA),
                  ),
                ),
                const SizedBox(height: 12),
                TextButton(
                  onPressed: () => ref.invalidate(goalsProvider),
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
          data: (goals) => Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
                child: Row(
                  children: [
                    GestureDetector(
                      onTap: () => context.pop(),
                      child: const Icon(
                        Icons.chevron_left_rounded,
                        size: 22,
                        color: Color(0xFF7B6EFF),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'My Goals',
                          style: GoogleFonts.nunito(
                            fontSize: 28,
                            fontWeight: FontWeight.w900,
                            color: const Color(0xFF1A1A2E),
                          ),
                        ),
                        Text(
                          goals.isEmpty ? 'No goals yet' : '${goals.length} active',
                          style: GoogleFonts.nunito(
                            fontSize: 14,
                            fontWeight: FontWeight.w700,
                            color: const Color(0xFF9090AA),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
              Expanded(
                child: RefreshIndicator(
                  color: const Color(0xFF7B6EFF),
                  onRefresh: () async => ref.invalidate(goalsProvider),
                  child: goals.isEmpty
                      ? EmptyIntentions(onAdd: () => context.pop())
                      : ListView.builder(
                          padding: const EdgeInsets.fromLTRB(16, 0, 16, 96),
                          itemCount: goals.length,
                          itemBuilder: (_, i) => GoalCard(
                            goal: goals[i],
                            onTap: () => context.push('/goals/${goals[i].pid}'),
                          ),
                        ),
                ),
              ),
            ],
          ),
        ),
      ),
      floatingActionButton: ConnieFab(onTap: () => context.pop()),
    );
  }
}
