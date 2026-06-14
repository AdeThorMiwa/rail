import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../../core/widgets/connie_fab.dart';
import '../providers/goals_provider.dart';
import '../widgets/goal_detail_body.dart';

class GoalDetailScreen extends ConsumerWidget {
  final String goalPid;

  const GoalDetailScreen({super.key, required this.goalPid});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(goalDetailProvider(goalPid));

    return Scaffold(
      backgroundColor: const Color(0xFFF4F8FF),
      body: state.when(
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
                onPressed: () => ref.invalidate(goalDetailProvider(goalPid)),
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
        data: (goal) => GoalDetailBody(
          goal: goal,
          onRefresh: () => ref.refresh(goalDetailProvider(goalPid).future),
        ),
      ),
      floatingActionButton: ConnieFab(
        onTap: () => context.push('/goals/$goalPid/chat'),
      ),
    );
  }
}
