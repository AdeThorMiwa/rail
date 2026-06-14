import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

const _quickReasons = [
  'Out of energy',
  'Not enough time',
  'Changed priorities',
  'Blocked by something',
];

class SkipTaskSheet extends StatefulWidget {
  final String taskTitle;
  final bool isFixed;
  final Future<void> Function(String? reason) onSubmit;

  const SkipTaskSheet({
    super.key,
    required this.taskTitle,
    this.isFixed = false,
    required this.onSubmit,
  });

  @override
  State<SkipTaskSheet> createState() => _SkipTaskSheetState();
}

class _SkipTaskSheetState extends State<SkipTaskSheet> {
  String? _selectedQuick;
  final _noteController = TextEditingController();
  bool _loading = false;

  @override
  void dispose() {
    _noteController.dispose();
    super.dispose();
  }

  String? get _resolvedReason {
    final note = _noteController.text.trim();
    return note.isNotEmpty ? note : _selectedQuick;
  }

  bool get _canSubmit => !widget.isFixed || _resolvedReason != null;

  Future<void> _submit() async {
    if (!_canSubmit) return;
    setState(() => _loading = true);
    try {
      await widget.onSubmit(_resolvedReason);
      if (mounted) Navigator.of(context).pop(true);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(
        left: 20,
        right: 20,
        bottom: MediaQuery.of(context).viewInsets.bottom + 36,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Center(
            child: Container(
              width: 32,
              height: 4,
              margin: const EdgeInsets.only(top: 12, bottom: 18),
              decoration: BoxDecoration(
                color: const Color(0xFFE8E2FF),
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          ),
          Text(
            widget.isFixed ? 'MISSING THIS?' : 'SKIPPING THIS?',
            style: GoogleFonts.nunito(
              fontSize: 11,
              fontWeight: FontWeight.w900,
              color: const Color(0xFFAAAAC0),
              letterSpacing: 1,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            widget.taskTitle,
            style: GoogleFonts.nunito(
              fontSize: 15,
              fontWeight: FontWeight.w900,
              color: const Color(0xFF1A1A2E),
              height: 1.3,
            ),
          ),
          const SizedBox(height: 16),
          Wrap(
            spacing: 7,
            runSpacing: 7,
            children: _quickReasons.map((r) {
              final sel = _selectedQuick == r;
              return GestureDetector(
                onTap: () => setState(() {
                  _selectedQuick = sel ? null : r;
                }),
                child: AnimatedContainer(
                  duration: const Duration(milliseconds: 150),
                  padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 7),
                  decoration: BoxDecoration(
                    color: sel ? const Color(0xFFFFF0F0) : Colors.white,
                    border: Border.all(
                      color: sel ? const Color(0xFFFF6B6B) : const Color(0xFFE8E2FF),
                      width: 2,
                    ),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Text(
                    r,
                    style: GoogleFonts.nunito(
                      fontSize: 12,
                      fontWeight: FontWeight.w800,
                      color: sel ? const Color(0xFFFF6B6B) : const Color(0xFF9090AA),
                    ),
                  ),
                ),
              );
            }).toList(),
          ),
          if (widget.isFixed) ...[
            const SizedBox(height: 6),
            Text(
              'Tell Connie why — she\'ll use this to plan better.',
              style: GoogleFonts.nunito(
                fontSize: 11,
                fontWeight: FontWeight.w700,
                color: const Color(0xFFAAAAC0),
              ),
            ),
          ],
          const SizedBox(height: 8),
          TextField(
            controller: _noteController,
            onChanged: (_) => setState(() {}),
            maxLines: 2,
            style: GoogleFonts.nunito(
              fontSize: 13,
              fontWeight: FontWeight.w600,
              color: const Color(0xFF1A1A2E),
            ),
            decoration: InputDecoration(
              hintText: 'Or describe what happened…',
              hintStyle: GoogleFonts.nunito(
                fontSize: 13,
                fontWeight: FontWeight.w600,
                color: const Color(0xFFC8C8DC),
              ),
              filled: true,
              fillColor: const Color(0xFFF4F8FF),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(10),
                borderSide: BorderSide.none,
              ),
              contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
            ),
          ),
          const SizedBox(height: 12),
          GestureDetector(
            onTap: (_loading || !_canSubmit) ? null : _submit,
            child: AnimatedContainer(
              duration: const Duration(milliseconds: 150),
              width: double.infinity,
              padding: const EdgeInsets.symmetric(vertical: 12),
              decoration: BoxDecoration(
                gradient: _canSubmit
                    ? const LinearGradient(
                        colors: [Color(0xFFFF6B6B), Color(0xFFFF9494)],
                      )
                    : null,
                color: _canSubmit ? null : const Color(0xFFE8E2FF),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Center(
                child: _loading
                    ? const SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(
                            color: Colors.white, strokeWidth: 2),
                      )
                    : Text(
                        widget.isFixed ? 'Mark as missed' : 'Skip task',
                        style: GoogleFonts.nunito(
                          fontSize: 14,
                          fontWeight: FontWeight.w900,
                          color: _canSubmit
                              ? Colors.white
                              : const Color(0xFFAAAAC0),
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
