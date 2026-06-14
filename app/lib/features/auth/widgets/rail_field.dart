import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class RailField extends StatefulWidget {
  final TextEditingController controller;
  final String label;
  final String hint;
  final TextInputType? keyboardType;
  final bool obscureText;
  final Widget? suffix;
  final Color focusBorderColor;
  final Color focusGlow;
  final String? Function(String?)? validator;

  const RailField({
    super.key,
    required this.controller,
    required this.label,
    required this.hint,
    this.keyboardType,
    this.obscureText = false,
    this.suffix,
    required this.focusBorderColor,
    required this.focusGlow,
    this.validator,
  });

  @override
  State<RailField> createState() => _RailFieldState();
}

class _RailFieldState extends State<RailField> {
  bool _focused = false;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          widget.label.toUpperCase(),
          style: GoogleFonts.nunito(
            fontSize: 12,
            fontWeight: FontWeight.w800,
            color: const Color(0xFFAAAAC0),
            letterSpacing: 0.8,
          ),
        ),
        const SizedBox(height: 8),
        Focus(
          onFocusChange: (f) => setState(() => _focused = f),
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 250),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(18),
              border: Border.all(
                color: _focused ? widget.focusBorderColor : Colors.transparent,
                width: 2.5,
              ),
              boxShadow: [
                BoxShadow(
                  color: _focused
                      ? widget.focusGlow.withValues(alpha: 0.22)
                      : Colors.black.withValues(alpha: 0.06),
                  blurRadius: _focused ? 20 : 8,
                  offset: const Offset(0, 3),
                ),
              ],
            ),
            child: TextFormField(
              controller: widget.controller,
              keyboardType: widget.keyboardType,
              obscureText: widget.obscureText,
              validator: widget.validator,
              style: GoogleFonts.nunito(
                fontSize: 15,
                fontWeight: FontWeight.w600,
                color: const Color(0xFF1A1A2E),
              ),
              decoration: InputDecoration(
                hintText: widget.hint,
                hintStyle: GoogleFonts.nunito(
                  fontSize: 15,
                  fontWeight: FontWeight.w600,
                  color: const Color(0xFFCCCCDD),
                ),
                suffixIcon: widget.suffix,
                border: InputBorder.none,
                contentPadding: const EdgeInsets.symmetric(
                  horizontal: 18,
                  vertical: 16,
                ),
                errorStyle: GoogleFonts.nunito(
                  fontSize: 12,
                  color: const Color(0xFFFF6B6B),
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }
}
