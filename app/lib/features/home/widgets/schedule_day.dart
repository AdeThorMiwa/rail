import 'dart:async';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../data/models/schedule_models.dart';
import 'now_task_card.dart';
import 'overdue_task_card.dart';
import 'pulse_dot.dart';
import 'schedule_progress_row.dart';
import 'upcoming_task_card.dart';

class ScheduleDay extends StatefulWidget {
  final DailySchedule schedule;
  final void Function(String taskTitle) onTaskActivated;

  const ScheduleDay({
    super.key,
    required this.schedule,
    required this.onTaskActivated,
  });

  @override
  State<ScheduleDay> createState() => _ScheduleDayState();
}

class _ScheduleDayState extends State<ScheduleDay> {
  Timer? _timer;
  bool _doneExpanded = false;
  final Set<String> _notifiedPids = {};

  @override
  void initState() {
    super.initState();
    _timer = Timer.periodic(const Duration(seconds: 30), (_) {
      if (mounted) {
        _checkForNewActiveEntries();
        setState(() {});
      }
    });
    WidgetsBinding.instance.addPostFrameCallback(
      (_) => _checkForNewActiveEntries(),
    );
  }

  void _checkForNewActiveEntries() {
    final now = TimeOfDay.now();
    for (final e in widget.schedule.taskEntries) {
      if (!e.isPending || _notifiedPids.contains(e.pid)) continue;
      if (e.timeStateAt(now) == ScheduleEntryTimeState.active) {
        _notifiedPids.add(e.pid);
        widget.onTaskActivated(e.task!.title);
      }
    }
  }

  @override
  void didUpdateWidget(ScheduleDay old) {
    super.didUpdateWidget(old);
    if (old.schedule != widget.schedule) {
      _notifiedPids.clear();
      WidgetsBinding.instance.addPostFrameCallback(
        (_) => _checkForNewActiveEntries(),
      );
    }
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final now = TimeOfDay.now();
    final taskEntries = widget.schedule.taskEntries;

    ScheduleEntry? activeEntry;
    final overdueEntries = <ScheduleEntry>[];
    final upcomingEntries = <ScheduleEntry>[];
    final doneEntries = <ScheduleEntry>[];

    for (final e in taskEntries) {
      if (e.isMissed) {
        overdueEntries.add(e);
        continue;
      }
      if (!e.isPending) {
        doneEntries.add(e);
        continue;
      }
      switch (e.timeStateAt(now)) {
        case ScheduleEntryTimeState.active:
          if (activeEntry == null) {
            activeEntry = e;
          } else {
            upcomingEntries.add(e);
          }
        case ScheduleEntryTimeState.overdue:
          overdueEntries.add(e);
        case ScheduleEntryTimeState.upcoming:
          upcomingEntries.add(e);
      }
    }

    final allDone =
        taskEntries.isNotEmpty && taskEntries.every((e) => !e.isPending);
    final doneCount = doneEntries.length;
    final totalCount = taskEntries.length;
    final hasContent =
        activeEntry != null ||
        overdueEntries.isNotEmpty ||
        upcomingEntries.isNotEmpty;

    return ListView(
      padding: const EdgeInsets.fromLTRB(0, 0, 0, 90),
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: ScheduleProgressRow(
            done: doneCount,
            total: totalCount,
            allDone: allDone,
          ),
        ),

        if (allDone) ...[
          _AllDoneBanner(schedule: widget.schedule),
        ] else ...[
          if (activeEntry == null &&
              widget.schedule.railNotes != null &&
              widget.schedule.railNotes!.isNotEmpty)
            _RailNoteStrip(note: widget.schedule.railNotes!),

          // Hint card when no active task but upcoming tasks exist
          if (activeEntry == null && upcomingEntries.isNotEmpty)
            _NextTaskHint(entry: upcomingEntries.first),

          // Overdue warning banner
          if (overdueEntries.isNotEmpty)
            _OverdueWarningBanner(count: overdueEntries.length),

          // NOW zone
          if (activeEntry != null) ...[
            _HappeningNowLabel(),
            const SizedBox(height: 10),
            NowTaskCard(entry: activeEntry),
            const SizedBox(height: 6),
          ],

          // COMING UP zone
          if (upcomingEntries.isNotEmpty) ...[
            _SectionLabel(
              label: activeEntry != null || overdueEntries.isNotEmpty
                  ? 'COMING UP'
                  : "TODAY'S SCHEDULE",
            ),
            ...upcomingEntries.map((e) => UpcomingTaskCard(entry: e)),
          ],

          if (!hasContent && doneEntries.isEmpty) const _EmptyScheduleNote(),
        ],

        // DONE accordion — always at bottom when entries exist
        if (doneEntries.isNotEmpty)
          _DoneAccordion(
            entries: doneEntries,
            expanded: _doneExpanded,
            onToggle: () => setState(() => _doneExpanded = !_doneExpanded),
          ),

        // OVERDUE zone
        if (overdueEntries.isNotEmpty) ...[
          _SectionLabel(
            label: 'OVERDUE',
            labelColor: const Color(0xFFEF5350),
            lineColor: const Color(0x26EF5350),
          ),
          ...overdueEntries.map((e) => OverdueTaskCard(entry: e)),
        ],
      ],
    );
  }
}

class _HappeningNowLabel extends StatelessWidget {
  const _HappeningNowLabel();

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
      child: Row(
        children: [
          const PulseDot(color: Color(0xFF4ADE80)),
          const SizedBox(width: 7),
          Text(
            'HAPPENING NOW',
            style: GoogleFonts.nunito(
              fontSize: 10,
              fontWeight: FontWeight.w900,
              color: const Color(0xFF4ADE80),
              letterSpacing: 1.2,
            ),
          ),
        ],
      ),
    );
  }
}

class _SectionLabel extends StatelessWidget {
  final String label;
  final Color labelColor;
  final Color lineColor;

  const _SectionLabel({
    required this.label,
    this.labelColor = const Color(0xFFAAAACC),
    this.lineColor = const Color(0x0F000000),
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 18, 16, 10),
      child: Row(
        children: [
          Text(
            label,
            style: GoogleFonts.nunito(
              fontSize: 11,
              fontWeight: FontWeight.w900,
              color: labelColor,
              letterSpacing: 1,
            ),
          ),
          const SizedBox(width: 10),
          Expanded(child: Container(height: 1.5, color: lineColor)),
        ],
      ),
    );
  }
}

class _NextTaskHint extends StatelessWidget {
  final ScheduleEntry entry;
  const _NextTaskHint({required this.entry});

  @override
  Widget build(BuildContext context) {
    final now = TimeOfDay.now();
    final nowMins = now.hour * 60 + now.minute;
    final startMins = entry.startTime.hour * 60 + entry.startTime.minute;
    final mins = startMins - nowMins;
    final countdown = mins <= 0
        ? 'starting now'
        : mins < 60
        ? 'in $mins minutes'
        : 'in ${mins ~/ 60}h ${mins % 60 > 0 ? '${mins % 60}m' : ''}';

    final h = entry.startTime.hourOfPeriod == 0
        ? 12
        : entry.startTime.hourOfPeriod;
    final m = entry.startTime.minute.toString().padLeft(2, '0');
    final p = entry.startTime.period == DayPeriod.am ? 'AM' : 'PM';
    final timeStr = '$h:$m $p';

    return Container(
      margin: const EdgeInsets.fromLTRB(16, 14, 16, 0),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: const Color(0xFFF0ECFF),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        children: [
          const Text('⏰', style: TextStyle(fontSize: 20)),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'FIRST TASK STARTS AT $timeStr',
                  style: GoogleFonts.nunito(
                    fontSize: 11,
                    fontWeight: FontWeight.w900,
                    color: const Color(0xFF6355EE),
                    letterSpacing: 0.3,
                  ),
                ),
                Text(
                  'Your alarm will ring $countdown',
                  style: GoogleFonts.nunito(
                    fontSize: 11,
                    fontWeight: FontWeight.w700,
                    color: const Color(0xFF9090AA),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _OverdueWarningBanner extends StatelessWidget {
  final int count;
  const _OverdueWarningBanner({required this.count});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.fromLTRB(16, 14, 16, 0),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: const Color(0xFFFFF0F0),
        border: Border.all(color: const Color(0xFFFFD6D6), width: 1.5),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        children: [
          const Text('⏰', style: TextStyle(fontSize: 18)),
          const SizedBox(width: 12),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                '$count ${count == 1 ? 'TASK' : 'TASKS'} OVERDUE',
                style: GoogleFonts.nunito(
                  fontSize: 11,
                  fontWeight: FontWeight.w900,
                  color: const Color(0xFFEF5350),
                ),
              ),
              Text(
                'Log ${count == 1 ? 'it' : 'them'} before ${count == 1 ? 'it\'s' : 'they\'re'} auto-missed',
                style: GoogleFonts.nunito(
                  fontSize: 11,
                  fontWeight: FontWeight.w700,
                  color: const Color(0xFFFFAAAA),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _AllDoneBanner extends StatelessWidget {
  final DailySchedule schedule;
  const _AllDoneBanner({required this.schedule});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.fromLTRB(16, 20, 16, 0),
      padding: const EdgeInsets.fromLTRB(20, 24, 20, 24),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [Color(0xFFF0ECFF), Color(0xFFE8F5FF)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        children: [
          const Text('🎉', style: TextStyle(fontSize: 32)),
          const SizedBox(height: 10),
          Text(
            'You cleared the board!',
            style: GoogleFonts.nunito(
              fontSize: 17,
              fontWeight: FontWeight.w900,
              color: const Color(0xFF1A1A2E),
            ),
          ),
          const SizedBox(height: 6),
          Text(
            "All tasks done for today. Rail will have\nyour schedule ready an hour before tomorrow.",
            textAlign: TextAlign.center,
            style: GoogleFonts.nunito(
              fontSize: 13,
              fontWeight: FontWeight.w600,
              color: const Color(0xFF9090AA),
              height: 1.6,
            ),
          ),
        ],
      ),
    );
  }
}

class _DoneAccordion extends StatelessWidget {
  final List<ScheduleEntry> entries;
  final bool expanded;
  final VoidCallback onToggle;

  const _DoneAccordion({
    required this.entries,
    required this.expanded,
    required this.onToggle,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Column(
        children: [
          GestureDetector(
            onTap: onToggle,
            behavior: HitTestBehavior.opaque,
            child: Padding(
              padding: const EdgeInsets.symmetric(vertical: 10),
              child: Row(
                children: [
                  Text(
                    'DONE',
                    style: GoogleFonts.nunito(
                      fontSize: 11,
                      fontWeight: FontWeight.w900,
                      color: const Color(0xFFAAAACC),
                      letterSpacing: 1,
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Container(
                      height: 1.5,
                      color: Colors.black.withValues(alpha: 0.06),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 8,
                      vertical: 2,
                    ),
                    decoration: BoxDecoration(
                      color: const Color(0xFFF0ECFF),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Text(
                      '${entries.length}',
                      style: GoogleFonts.nunito(
                        fontSize: 10,
                        fontWeight: FontWeight.w900,
                        color: const Color(0xFF6355EE),
                      ),
                    ),
                  ),
                  const SizedBox(width: 6),
                  AnimatedRotation(
                    turns: expanded ? 0.5 : 0,
                    duration: const Duration(milliseconds: 200),
                    child: const Icon(
                      Icons.keyboard_arrow_down_rounded,
                      size: 18,
                      color: Color(0xFFAAAACC),
                    ),
                  ),
                ],
              ),
            ),
          ),
          AnimatedSize(
            duration: const Duration(milliseconds: 250),
            curve: Curves.easeInOut,
            child: expanded
                ? Column(
                    children: entries.map((e) => _DoneEntry(entry: e)).toList(),
                  )
                : const SizedBox.shrink(),
          ),
        ],
      ),
    );
  }
}

class _DoneEntry extends StatelessWidget {
  final ScheduleEntry entry;
  const _DoneEntry({required this.entry});

  @override
  Widget build(BuildContext context) {
    final task = entry.task;
    final isSkippedOrMissed = entry.isSkipped || entry.isMissed;

    return Opacity(
      opacity: 0.55,
      child: Container(
        margin: const EdgeInsets.only(bottom: 8),
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(10),
          border: Border.all(
            color: isSkippedOrMissed
                ? const Color(0xFFFFE0E0)
                : const Color(0xFFF0EFF8),
            width: 1.5,
          ),
        ),
        child: Row(
          children: [
            Icon(
              entry.isCompleted ? Icons.check_rounded : Icons.close_rounded,
              size: 17,
              color: entry.isCompleted
                  ? const Color(0xFF4ADE80)
                  : const Color(0xFFEF5350),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    task?.title ?? '',
                    style: GoogleFonts.nunito(
                      fontSize: 13,
                      fontWeight: FontWeight.w800,
                      color: const Color(0xFF1A1A2E),
                      decoration: TextDecoration.lineThrough,
                      decorationColor: isSkippedOrMissed
                          ? const Color(0xFFFFAAAA)
                          : const Color(0xFFAAAACC),
                    ),
                    overflow: TextOverflow.ellipsis,
                  ),
                  Text(
                    _statusLabel(),
                    style: GoogleFonts.nunito(
                      fontSize: 10,
                      fontWeight: FontWeight.w700,
                      color: const Color(0xFFAAAACC),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  String _statusLabel() {
    if (entry.isCompleted) return 'Completed';
    if (entry.isSkipped) return 'Skipped';
    if (entry.isMissed) return 'Missed';
    return '';
  }
}

class _RailNoteStrip extends StatelessWidget {
  final String note;
  const _RailNoteStrip({required this.note});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.fromLTRB(16, 14, 16, 0),
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 9),
      decoration: BoxDecoration(
        color: const Color(0xFF6355EE).withValues(alpha: 0.07),
        borderRadius: BorderRadius.circular(10),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '✦',
            style: GoogleFonts.nunito(
              fontSize: 12,
              color: const Color(0xFF6355EE).withValues(alpha: 0.6),
              height: 1.5,
            ),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              note,
              style: GoogleFonts.nunito(
                fontSize: 12,
                fontWeight: FontWeight.w700,
                fontStyle: FontStyle.italic,
                color: const Color(0xFF6355EE),
                height: 1.45,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _EmptyScheduleNote extends StatelessWidget {
  const _EmptyScheduleNote();

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 24, 16, 0),
      child: Text(
        'No tasks scheduled yet for today.',
        style: GoogleFonts.nunito(
          fontSize: 13,
          fontWeight: FontWeight.w600,
          color: const Color(0xFF9090AA),
        ),
      ),
    );
  }
}
