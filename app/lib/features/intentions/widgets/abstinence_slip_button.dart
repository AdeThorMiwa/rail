import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import '../providers/goals_provider.dart';

class AbstinenceSlipButton extends ConsumerStatefulWidget {
  final String goalPid;

  const AbstinenceSlipButton({super.key, required this.goalPid});

  @override
  ConsumerState<AbstinenceSlipButton> createState() => _AbstinenceSlipButtonState();
}

class _AbstinenceSlipButtonState extends ConsumerState<AbstinenceSlipButton> {
  bool _loading = false;
  bool _logged = false;

  Future<void> _onTap() async {
    setState(() => _loading = true);
    try {
      await ref.read(goalsRepositoryProvider).slipGoal(widget.goalPid);
      if (mounted) {
        setState(() => _logged = true);
        ref.invalidate(goalDetailProvider(widget.goalPid));
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Center(
      child: OutlinedButton(
        onPressed: (_logged || _loading) ? null : _onTap,
        style: OutlinedButton.styleFrom(
          side: const BorderSide(color: Color(0xFFFFD0D0), width: 1.5),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(20),
          ),
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
          foregroundColor: const Color(0xFFFF9494),
          disabledForegroundColor:
              const Color(0xFFFF9494).withValues(alpha: 0.5),
        ),
        child: _loading
            ? const SizedBox(
                width: 16,
                height: 16,
                child: CircularProgressIndicator(
                  color: Color(0xFFFF9494),
                  strokeWidth: 2,
                ),
              )
            : Text(
                _logged ? 'Logged. Streak resets tomorrow.' : 'I slipped today',
                style: GoogleFonts.nunito(
                  fontSize: 13,
                  fontWeight: FontWeight.w800,
                ),
              ),
      ),
    );
  }
}
