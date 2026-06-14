import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class CompleteTaskSheet extends StatefulWidget {
  final String taskTitle;
  final bool hasTaskTarget;
  final double? estimatedValue;
  final String? targetUnit;
  final Future<void> Function({
    required String completionType,
    String? completionNote,
    double? actualValue,
  }) onSubmit;

  const CompleteTaskSheet({
    super.key,
    required this.taskTitle,
    required this.hasTaskTarget,
    this.estimatedValue,
    this.targetUnit,
    required this.onSubmit,
  });

  @override
  State<CompleteTaskSheet> createState() => _CompleteTaskSheetState();
}

class _CompleteTaskSheetState extends State<CompleteTaskSheet> {
  String _completionType = 'FULL';
  final _noteController = TextEditingController();
  final _partialController = TextEditingController();
  bool _loading = false;
  late double _actualValue;

  @override
  void initState() {
    super.initState();
    _actualValue = widget.estimatedValue ?? 1;
  }

  @override
  void dispose() {
    _noteController.dispose();
    _partialController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    setState(() => _loading = true);
    try {
      final partialNote = _partialController.text.trim();
      final note = _noteController.text.trim();
      final combined = [
        if (_completionType == 'PARTIAL' && partialNote.isNotEmpty) partialNote,
        if (note.isNotEmpty) note,
      ].join('\n\n');

      await widget.onSubmit(
        completionType: _completionType,
        completionNote: combined.isEmpty ? null : combined,
        actualValue: widget.hasTaskTarget ? _actualValue : null,
      );
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
            "HOW'D IT GO? 🎯",
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
          Row(
            children: [
              Expanded(child: _TypeButton(
                label: 'Crushed it',
                icon: Icons.check_rounded,
                selected: _completionType == 'FULL',
                onTap: () => setState(() => _completionType = 'FULL'),
              )),
              const SizedBox(width: 8),
              Expanded(child: _TypeButton(
                label: 'Made a start',
                selected: _completionType == 'PARTIAL',
                onTap: () => setState(() => _completionType = 'PARTIAL'),
              )),
            ],
          ),
          if (_completionType == 'PARTIAL') ...[
            const SizedBox(height: 14),
            _SheetTextField(
              controller: _partialController,
              placeholder: 'What got in the way?',
            ),
          ],
          if (widget.hasTaskTarget) ...[
            const SizedBox(height: 18),
            _ActualValueStepper(
              value: _actualValue,
              unit: widget.targetUnit,
              estimatedValue: widget.estimatedValue,
              onChanged: (v) => setState(() => _actualValue = v),
            ),
          ],
          const SizedBox(height: 14),
          _SheetTextField(
            controller: _noteController,
            placeholder: 'Anything to remember for next time? (optional)',
          ),
          const SizedBox(height: 4),
          _SubmitButton(
            label: 'Done',
            icon: Icons.chevron_right_rounded,
            loading: _loading,
            color1: const Color(0xFF22C55E),
            color2: const Color(0xFF34D399),
            onTap: _submit,
          ),
        ],
      ),
    );
  }
}

class _ActualValueStepper extends StatelessWidget {
  final double value;
  final String? unit;
  final double? estimatedValue;
  final ValueChanged<double> onChanged;

  const _ActualValueStepper({
    required this.value,
    this.unit,
    this.estimatedValue,
    required this.onChanged,
  });

  String _formatValue(double v) =>
      v == v.truncateToDouble() ? v.toInt().toString() : v.toStringAsFixed(1);

  double get _step => (estimatedValue != null && estimatedValue! > 10) ? 1 : 0.5;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            _AdjButton(
              label: '−',
              onTap: () => onChanged((value - _step).clamp(0, double.infinity)),
            ),
            const SizedBox(width: 24),
            Column(
              children: [
                Text(
                  _formatValue(value),
                  style: GoogleFonts.nunito(
                    fontSize: 40,
                    fontWeight: FontWeight.w900,
                    color: const Color(0xFF1A1A2E),
                    height: 1,
                  ),
                ),
                if (unit != null)
                  Text(
                    unit!,
                    style: GoogleFonts.nunito(
                      fontSize: 13,
                      fontWeight: FontWeight.w800,
                      color: const Color(0xFFAAAAC0),
                    ),
                  ),
              ],
            ),
            const SizedBox(width: 24),
            _AdjButton(
              label: '+',
              onTap: () => onChanged(value + _step),
            ),
          ],
        ),
        if (estimatedValue != null) ...[
          const SizedBox(height: 8),
          Text(
            'Rail estimated ${_formatValue(estimatedValue!)}${unit != null ? ' $unit' : ''} for this session',
            textAlign: TextAlign.center,
            style: GoogleFonts.nunito(
              fontSize: 11,
              fontWeight: FontWeight.w700,
              color: const Color(0xFFC8C0E8),
            ),
          ),
        ],
      ],
    );
  }
}

class _AdjButton extends StatelessWidget {
  final String label;
  final VoidCallback onTap;

  const _AdjButton({required this.label, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        width: 42,
        height: 42,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          border: Border.all(color: const Color(0xFFE8E2FF), width: 2),
          color: Colors.white,
        ),
        child: Center(
          child: Text(
            label,
            style: GoogleFonts.nunito(
              fontSize: 22,
              fontWeight: FontWeight.w900,
              color: const Color(0xFF7B6EFF),
              height: 1,
            ),
          ),
        ),
      ),
    );
  }
}

class _TypeButton extends StatelessWidget {
  final String label;
  final IconData? icon;
  final bool selected;
  final VoidCallback onTap;

  const _TypeButton({required this.label, required this.selected, required this.onTap, this.icon});

  @override
  Widget build(BuildContext context) {
    final color = selected ? const Color(0xFF7B6EFF) : const Color(0xFFAAAAC0);
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 150),
        padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 8),
        decoration: BoxDecoration(
          color: selected ? const Color(0xFFF0ECFF) : Colors.white,
          border: Border.all(
            color: selected ? const Color(0xFF7B6EFF) : const Color(0xFFE8E2FF),
            width: 2,
          ),
          borderRadius: BorderRadius.circular(10),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              label,
              textAlign: TextAlign.center,
              style: GoogleFonts.nunito(
                fontSize: 13,
                fontWeight: FontWeight.w800,
                color: color,
              ),
            ),
            if (icon != null) ...[
              const SizedBox(width: 4),
              Icon(icon, size: 13, color: color),
            ],
          ],
        ),
      ),
    );
  }
}

class _SheetTextField extends StatelessWidget {
  final TextEditingController controller;
  final String placeholder;

  const _SheetTextField({required this.controller, required this.placeholder});

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: controller,
      maxLines: 2,
      style: GoogleFonts.nunito(
        fontSize: 13,
        fontWeight: FontWeight.w600,
        color: const Color(0xFF1A1A2E),
      ),
      decoration: InputDecoration(
        hintText: placeholder,
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
    );
  }
}

class _SubmitButton extends StatelessWidget {
  final String label;
  final IconData? icon;
  final bool loading;
  final Color color1;
  final Color color2;
  final VoidCallback onTap;

  const _SubmitButton({
    required this.label,
    required this.loading,
    required this.color1,
    required this.color2,
    required this.onTap,
    this.icon,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: loading ? null : onTap,
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.symmetric(vertical: 12),
        decoration: BoxDecoration(
          gradient: LinearGradient(colors: [color1, color2]),
          borderRadius: BorderRadius.circular(12),
        ),
        child: Center(
          child: loading
              ? const SizedBox(
                  width: 18,
                  height: 18,
                  child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2),
                )
              : Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      label,
                      style: GoogleFonts.nunito(
                        fontSize: 14,
                        fontWeight: FontWeight.w900,
                        color: Colors.white,
                      ),
                    ),
                    if (icon != null) ...[
                      const SizedBox(width: 6),
                      Icon(icon, size: 15, color: Colors.white),
                    ],
                  ],
                ),
        ),
      ),
    );
  }
}
