import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class SwipeToReply extends StatefulWidget {
  final Widget child;
  final VoidCallback onReply;

  const SwipeToReply({required this.child, required this.onReply, super.key});

  @override
  State<SwipeToReply> createState() => _SwipeToReplyState();
}

class _SwipeToReplyState extends State<SwipeToReply>
    with SingleTickerProviderStateMixin {
  static const double _threshold = 56.0;
  static const double _maxDrag = 72.0;

  late final AnimationController _snapBack;
  double _dragOffset = 0;
  bool _triggered = false;

  @override
  void initState() {
    super.initState();
    _snapBack = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 220),
    );
  }

  @override
  void dispose() {
    _snapBack.dispose();
    super.dispose();
  }

  void _onDragUpdate(DragUpdateDetails d) {
    if (d.delta.dx < 0) return;
    setState(() {
      _dragOffset = (_dragOffset + d.delta.dx).clamp(0, _maxDrag);
    });

    if (!_triggered && _dragOffset >= _threshold) {
      _triggered = true;
      HapticFeedback.lightImpact();
      widget.onReply();
    }
  }

  void _onDragEnd(DragEndDetails _) {
    _triggered = false;
    final start = _dragOffset;
    final anim = Tween<double>(begin: start, end: 0).animate(
      CurvedAnimation(parent: _snapBack, curve: Curves.easeOut),
    );
    anim.addListener(() => setState(() => _dragOffset = anim.value));
    _snapBack.forward(from: 0);
  }

  @override
  Widget build(BuildContext context) {
    final iconOpacity = (_dragOffset / _threshold).clamp(0.0, 1.0);

    return GestureDetector(
      onHorizontalDragUpdate: _onDragUpdate,
      onHorizontalDragEnd: _onDragEnd,
      child: Stack(
        clipBehavior: Clip.none,
        children: [
          Positioned.fill(
            child: Align(
              alignment: Alignment.centerLeft,
              child: Opacity(
                opacity: iconOpacity,
                child: const Padding(
                  padding: EdgeInsets.only(left: 4),
                  child: Icon(
                    Icons.reply_rounded,
                    size: 20,
                    color: Color(0xFF9090AA),
                  ),
                ),
              ),
            ),
          ),
          Transform.translate(
            offset: Offset(_dragOffset, 0),
            child: widget.child,
          ),
        ],
      ),
    );
  }
}
