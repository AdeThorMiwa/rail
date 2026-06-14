import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class SchedulingSubmitButton extends StatefulWidget {
  final bool isLoading;
  final VoidCallback? onTap;

  const SchedulingSubmitButton({
    super.key,
    required this.isLoading,
    required this.onTap,
  });

  @override
  State<SchedulingSubmitButton> createState() => _SchedulingSubmitButtonState();
}

class _SchedulingSubmitButtonState extends State<SchedulingSubmitButton>
    with SingleTickerProviderStateMixin {
  late final AnimationController _ctrl;
  late final Animation<double> _scale;

  @override
  void initState() {
    super.initState();
    _ctrl = AnimationController(
        duration: const Duration(milliseconds: 120), vsync: this);
    _scale = Tween<double>(begin: 1, end: 0.95)
        .animate(CurvedAnimation(parent: _ctrl, curve: Curves.easeIn));
  }

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTapDown: widget.onTap != null ? (_) => _ctrl.forward() : null,
      onTapUp: widget.onTap != null
          ? (_) {
              _ctrl.reverse();
              widget.onTap!();
            }
          : null,
      onTapCancel: () => _ctrl.reverse(),
      child: ScaleTransition(
        scale: _scale,
        child: Container(
          width: double.infinity,
          height: 56,
          decoration: BoxDecoration(
            gradient: LinearGradient(
              colors: widget.onTap != null
                  ? const [Color(0xFF6355EE), Color(0xFF9B8FFF)]
                  : [
                      const Color(0xFF6355EE).withValues(alpha: 0.5),
                      const Color(0xFF9B8FFF).withValues(alpha: 0.5),
                    ],
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
            borderRadius: BorderRadius.circular(14),
            boxShadow: widget.onTap != null
                ? [
                    BoxShadow(
                      color: const Color(0xFF7B6EFF).withValues(alpha: 0.4),
                      blurRadius: 28,
                      offset: const Offset(0, 10),
                    ),
                  ]
                : null,
          ),
          child: Center(
            child: widget.isLoading
                ? const SizedBox(
                    width: 22,
                    height: 22,
                    child: CircularProgressIndicator(
                        strokeWidth: 2.5, color: Colors.white),
                  )
                : Text(
                    "Let's go 🚂",
                    style: GoogleFonts.nunito(
                      fontSize: 17,
                      fontWeight: FontWeight.w800,
                      color: Colors.white,
                      letterSpacing: 0.2,
                    ),
                  ),
          ),
        ),
      ),
    );
  }
}
