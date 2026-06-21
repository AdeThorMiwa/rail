import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import '../providers/goals_provider.dart';

class GoalCompleteCta extends ConsumerStatefulWidget {
  final String goalPid;

  const GoalCompleteCta({super.key, required this.goalPid});

  @override
  ConsumerState<GoalCompleteCta> createState() => _GoalCompleteCtaState();
}

class _GoalCompleteCtaState extends ConsumerState<GoalCompleteCta> {
  bool _loading = false;

  Future<void> _onTap() async {
    final notes = await _showNotesSheet();
    if (!mounted) return;

    setState(() => _loading = true);
    try {
      await ref
          .read(goalsProvider.notifier)
          .completeGoal(widget.goalPid, notes: notes);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<String?> _showNotesSheet() async {
    final controller = TextEditingController();
    return showModalBottomSheet<String>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => Padding(
        padding: EdgeInsets.only(
          bottom: MediaQuery.of(context).viewInsets.bottom,
        ),
        child: Container(
          decoration: const BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
          ),
          padding: const EdgeInsets.fromLTRB(20, 20, 20, 32),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Any notes before you close this out?',
                style: GoogleFonts.nunito(
                  fontSize: 15,
                  fontWeight: FontWeight.w800,
                  color: const Color(0xFF1A1A2E),
                ),
              ),
              const SizedBox(height: 4),
              Text(
                'What worked, what didn\'t, what you\'d do differently — Rail will use this to shape the next goal.',
                style: GoogleFonts.nunito(
                  fontSize: 12,
                  color: const Color(0xFF9090AA),
                ),
              ),
              const SizedBox(height: 14),
              TextField(
                controller: controller,
                autofocus: true,
                maxLines: 4,
                style: GoogleFonts.nunito(fontSize: 14),
                decoration: InputDecoration(
                  hintText: 'Optional — skip if nothing to add',
                  hintStyle: GoogleFonts.nunito(
                    color: const Color(0xFFBBBBCC),
                    fontSize: 13,
                  ),
                  filled: true,
                  fillColor: const Color(0xFFF4F8FF),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: BorderSide.none,
                  ),
                ),
              ),
              const SizedBox(height: 16),
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton(
                      onPressed: () => Navigator.pop(context, null),
                      style: OutlinedButton.styleFrom(
                        side: const BorderSide(color: Color(0xFFE0E0F0)),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(24),
                        ),
                      ),
                      child: Text(
                        'Skip',
                        style: GoogleFonts.nunito(
                          fontWeight: FontWeight.w700,
                          color: const Color(0xFF9090AA),
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: DecoratedBox(
                      decoration: BoxDecoration(
                        gradient: const LinearGradient(
                          colors: [Color(0xFF22C55E), Color(0xFF34D399)],
                        ),
                        borderRadius: BorderRadius.circular(24),
                      ),
                      child: TextButton(
                        onPressed: () =>
                            Navigator.pop(context, controller.text.trim()),
                        child: Text(
                          'Complete goal 🏆',
                          style: GoogleFonts.nunito(
                            fontWeight: FontWeight.w900,
                            color: Colors.white,
                          ),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 14, 16, 14),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [Color(0xFFF0FFF4), Color(0xFFE8F8EF)],
        ),
        border: Border.all(color: const Color(0xFFBBF7D0), width: 1.5),
        borderRadius: BorderRadius.circular(14),
      ),
      child: Column(
        children: [
          Text(
            'All milestones done?',
            style: GoogleFonts.nunito(
              fontSize: 12,
              fontWeight: FontWeight.w800,
              color: const Color(0xFF22A65A),
            ),
          ),
          const SizedBox(height: 8),
          SizedBox(
            width: double.infinity,
            child: _loading
                ? const Center(
                    child: SizedBox(
                      height: 24,
                      width: 24,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Color(0xFF22C55E),
                      ),
                    ),
                  )
                : DecoratedBox(
                    decoration: BoxDecoration(
                      gradient: const LinearGradient(
                        colors: [Color(0xFF22C55E), Color(0xFF34D399)],
                      ),
                      borderRadius: BorderRadius.circular(24),
                    ),
                    child: TextButton(
                      onPressed: _onTap,
                      child: Text(
                        'Mark goal complete 🏆',
                        style: GoogleFonts.nunito(
                          fontWeight: FontWeight.w900,
                          color: Colors.white,
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
