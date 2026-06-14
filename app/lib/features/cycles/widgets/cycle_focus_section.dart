import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import '../data/models/cycle_models.dart';
import '../providers/cycle_provider.dart';

class CycleFocusSection extends ConsumerWidget {
  final UserCycle cycle;

  const CycleFocusSection({super.key, required this.cycle});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final focusesAsync = ref.watch(cycleFocusesProvider(cycle.pid));

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'FOCUS GOALS',
          style: GoogleFonts.nunito(
            fontSize: 11,
            fontWeight: FontWeight.w900,
            color: const Color(0xFFAAAAC0),
            letterSpacing: 1,
          ),
        ),
        const SizedBox(height: 10),
        focusesAsync.when(
          loading: () => _loadingCard(),
          error: (_, __) => _emptyCard(context),
          data: (focuses) =>
              focuses.isEmpty ? _emptyCard(context) : _focusList(context, focuses),
        ),
      ],
    );
  }

  Widget _focusList(BuildContext context, List<CycleFocusGoal> focuses) {
    return Column(
      children: [
        ...focuses.map((f) => Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: _FocusGoalTile(goal: f, cycle: cycle),
            )),
        const SizedBox(height: 4),
        GestureDetector(
          onTap: () => context.push(
            '/chat/CYCLE/${cycle.pid}?title=${Uri.encodeComponent(cycle.title)}',
          ),
          child: Container(
            padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 14),
            decoration: BoxDecoration(
              color: const Color(0xFFF0EEFF),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.edit_rounded, size: 13, color: Color(0xFF7B6EFF)),
                const SizedBox(width: 6),
                Text(
                  'Edit focus goals',
                  style: GoogleFonts.nunito(
                    fontSize: 12,
                    fontWeight: FontWeight.w800,
                    color: const Color(0xFF7B6EFF),
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _emptyCard(BuildContext context) {
    return GestureDetector(
      onTap: () => context.push(
        '/chat/CYCLE/${cycle.pid}?title=${Uri.encodeComponent(cycle.title)}',
      ),
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: const Color(0xFFE8E2FF), width: 1.5),
        ),
        child: Row(
          children: [
            const Text('🎯', style: TextStyle(fontSize: 20)),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'No focus goals yet',
                    style: GoogleFonts.nunito(
                      fontSize: 14,
                      fontWeight: FontWeight.w800,
                      color: const Color(0xFF1A1A2E),
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    'Chat with Connie to set your focus goals for this cycle.',
                    style: GoogleFonts.nunito(
                      fontSize: 12,
                      fontWeight: FontWeight.w600,
                      color: const Color(0xFF9090AA),
                      height: 1.4,
                    ),
                  ),
                ],
              ),
            ),
            const Icon(
              Icons.arrow_forward_ios_rounded,
              size: 14,
              color: Color(0xFF7B6EFF),
            ),
          ],
        ),
      ),
    );
  }

  Widget _loadingCard() {
    return Container(
      height: 56,
      decoration: BoxDecoration(
        color: const Color(0xFFF4F2FF),
        borderRadius: BorderRadius.circular(12),
      ),
    );
  }
}

class _FocusGoalTile extends StatelessWidget {
  final CycleFocusGoal goal;
  final UserCycle cycle;

  const _FocusGoalTile({required this.goal, required this.cycle});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: const Color(0xFFE8E2FF), width: 1.5),
      ),
      child: Row(
        children: [
          Container(
            width: 32,
            height: 32,
            decoration: BoxDecoration(
              color: const Color(0xFFEDE9FF),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Center(
              child: Text(
                _typeEmoji(goal.type),
                style: const TextStyle(fontSize: 15),
              ),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  goal.title,
                  style: GoogleFonts.nunito(
                    fontSize: 14,
                    fontWeight: FontWeight.w800,
                    color: const Color(0xFF1A1A2E),
                  ),
                ),
                Text(
                  goal.type,
                  style: GoogleFonts.nunito(
                    fontSize: 11,
                    fontWeight: FontWeight.w700,
                    color: const Color(0xFF9090AA),
                    letterSpacing: 0.3,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  String _typeEmoji(String type) => switch (type) {
        'habit' => '🔁',
        'abstinence' => '🚫',
        'project' => '🗂️',
        'quantified' => '📊',
        _ => '✅',
      };
}
