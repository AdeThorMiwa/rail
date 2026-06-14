import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../data/models/goal_models.dart';
import 'milestone_pulsing_node.dart';
import 'task_timeline_row.dart';

class MilestoneTimelineItem extends StatefulWidget {
  final MilestoneDetail milestone;
  final bool isLast;

  const MilestoneTimelineItem({
    super.key,
    required this.milestone,
    this.isLast = false,
  });

  @override
  State<MilestoneTimelineItem> createState() => _MilestoneTimelineItemState();
}

class _MilestoneTimelineItemState extends State<MilestoneTimelineItem>
    with SingleTickerProviderStateMixin {
  bool _expanded = false;
  late final AnimationController _ctrl;
  late final Animation<double> _anim;

  @override
  void initState() {
    super.initState();
    _expanded = widget.milestone.isActive;
    _ctrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 350),
      value: _expanded ? 1.0 : 0.0,
    );
    _anim = CurvedAnimation(parent: _ctrl, curve: Curves.easeInOutCubic);
  }

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  void _toggle() {
    setState(() => _expanded = !_expanded);
    _expanded ? _ctrl.forward() : _ctrl.reverse();
  }

  @override
  Widget build(BuildContext context) {
    final m = widget.milestone;

    return Row(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SizedBox(
          width: 20,
          child: Column(
            children: [
              _buildNode(),
              if (!widget.isLast)
                Expanded(
                  child: Container(
                    width: 2,
                    margin: const EdgeInsets.only(top: 4),
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(1),
                      gradient: m.isDone
                          ? const LinearGradient(
                              begin: Alignment.topCenter,
                              end: Alignment.bottomCenter,
                              colors: [Color(0xFF9B8FFF), Color(0xFFDDD8F0)],
                            )
                          : null,
                      color: m.isDone ? null : const Color(0xFFEEE8FF),
                    ),
                  ),
                ),
            ],
          ),
        ),
        const SizedBox(width: 14),
        Expanded(
          child: GestureDetector(
            onTap: m.tasks.isNotEmpty ? _toggle : null,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Padding(
                  padding: const EdgeInsets.only(bottom: 10),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        m.title,
                        style: GoogleFonts.nunito(
                          fontSize: m.isActive ? 15 : 14,
                          fontWeight: FontWeight.w800,
                          color: m.isDone
                              ? const Color(0xFF9090AA)
                              : const Color(0xFF1A1A2E),
                          decoration:
                              m.isDone ? TextDecoration.lineThrough : null,
                        ),
                      ),
                      const SizedBox(height: 3),
                      Text(
                        _subLabel(),
                        style: GoogleFonts.nunito(
                          fontSize: 12,
                          fontWeight: FontWeight.w700,
                          color: m.isDone
                              ? const Color(0xFF5BBFB8)
                              : m.isActive
                                  ? const Color(0xFF7B6EFF)
                                  : const Color(0xFFAAAAC0),
                        ),
                      ),
                    ],
                  ),
                ),
                SizeTransition(
                  sizeFactor: _anim,
                  child: Padding(
                    padding: const EdgeInsets.only(bottom: 16),
                    child: Column(
                      children:
                          m.tasks.map((t) => TaskTimelineRow(task: t)).toList(),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildNode() {
    final m = widget.milestone;
    if (m.isActive) return const MilestonePulsingNode();
    return Container(
      width: 14,
      height: 14,
      margin: const EdgeInsets.only(top: 2),
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        gradient: m.isDone
            ? const LinearGradient(
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: [Color(0xFF7B6EFF), Color(0xFF9B8FFF)],
              )
            : null,
        color: m.isDone ? null : Colors.white,
        border: m.isDone
            ? null
            : Border.all(color: const Color(0xFFDDD8F0), width: 2.5),
        boxShadow: m.isDone
            ? [
                BoxShadow(
                  color: const Color(0xFF7B6EFF).withValues(alpha: 0.3),
                  blurRadius: 8,
                  offset: const Offset(0, 2),
                )
              ]
            : null,
      ),
    );
  }

  String _subLabel() {
    final m = widget.milestone;
    if (m.isDone) return 'Done';
    if (m.isActive) {
      final pending = m.pendingTaskCount;
      return '$pending task${pending == 1 ? '' : 's'} remaining';
    }
    final total = m.tasks.length;
    return total == 0
        ? 'Upcoming'
        : '$total task${total == 1 ? '' : 's'} · upcoming';
  }
}
