import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import '../providers/cycle_provider.dart';
import 'cycle_date_row.dart';
import 'cycle_hero_blurb.dart';
import 'cycle_time_row.dart';

class CreateCycleForm extends ConsumerStatefulWidget {
  const CreateCycleForm({super.key});

  @override
  ConsumerState<CreateCycleForm> createState() => _CreateCycleFormState();
}

class _CreateCycleFormState extends ConsumerState<CreateCycleForm> {
  late DateTime _startDate;
  late DateTime _endDate;
  int _reviewHour = 19;
  int _reviewMinute = 0;
  bool _loading = false;
  String? _error;
  final _titleController = TextEditingController();

  @override
  void initState() {
    super.initState();
    final today = DateTime.now();
    _startDate = DateTime(today.year, today.month, today.day);
    _endDate = _startDate.add(const Duration(days: 7));
  }

  @override
  void dispose() {
    _titleController.dispose();
    super.dispose();
  }

  String _defaultTitle() {
    const months = [
      'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
      'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec',
    ];
    return '${months[_startDate.month - 1]} ${_startDate.day} – '
        '${months[_endDate.month - 1]} ${_endDate.day}';
  }

  Future<void> _pickDate({required bool isStart}) async {
    final first = isStart ? DateTime.now() : _startDate.add(const Duration(days: 1));
    final last = isStart
        ? DateTime.now().add(const Duration(days: 13))
        : _startDate.add(const Duration(days: 14));
    final picked = await showDatePicker(
      context: context,
      initialDate: isStart ? _startDate : _endDate,
      firstDate: first,
      lastDate: last,
      builder: (ctx, child) => Theme(
        data: Theme.of(ctx).copyWith(
          colorScheme: const ColorScheme.light(primary: Color(0xFF7B6EFF)),
        ),
        child: child!,
      ),
    );
    if (picked == null) return;
    setState(() {
      if (isStart) {
        _startDate = picked;
        if (_endDate.isBefore(_startDate.add(const Duration(days: 1)))) {
          _endDate = _startDate.add(const Duration(days: 7));
        }
        if (_endDate.isAfter(_startDate.add(const Duration(days: 14)))) {
          _endDate = _startDate.add(const Duration(days: 7));
        }
      } else {
        _endDate = picked;
      }
    });
  }

  Future<void> _pickTime() async {
    final picked = await showTimePicker(
      context: context,
      initialTime: TimeOfDay(hour: _reviewHour, minute: _reviewMinute),
      builder: (ctx, child) => Theme(
        data: Theme.of(ctx).copyWith(
          colorScheme: const ColorScheme.light(primary: Color(0xFF7B6EFF)),
        ),
        child: child!,
      ),
    );
    if (picked == null) return;
    setState(() {
      _reviewHour = picked.hour;
      _reviewMinute = picked.minute;
    });
  }

  Future<void> _create() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final title = _titleController.text.trim();
      final cycle = await ref.read(cycleProvider.notifier).create(
            startDate: _startDate,
            endDate: _endDate,
            reviewHour: _reviewHour,
            reviewMinute: _reviewMinute,
            title: title.isNotEmpty ? title : null,
          );
      if (mounted) {
        context.push('/chat/CYCLE/${cycle.pid}?title=${Uri.encodeComponent(cycle.title)}');
      }
    } catch (_) {
      setState(() => _error = 'Something went wrong. Please try again.');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 0, 20, 32),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const CycleHeroBlurb(),
          const SizedBox(height: 20),
          TextField(
            controller: _titleController,
            onChanged: (_) => setState(() {}),
            style: GoogleFonts.nunito(
              fontSize: 15,
              fontWeight: FontWeight.w800,
              color: const Color(0xFF1A1A2E),
            ),
            decoration: InputDecoration(
              hintText: _defaultTitle(),
              hintStyle: GoogleFonts.nunito(
                fontSize: 15,
                fontWeight: FontWeight.w700,
                color: const Color(0xFFCCCCE0),
              ),
              labelText: 'CYCLE TITLE',
              labelStyle: GoogleFonts.nunito(
                fontSize: 10,
                fontWeight: FontWeight.w800,
                color: const Color(0xFFAAAAC0),
                letterSpacing: 0.8,
              ),
              floatingLabelBehavior: FloatingLabelBehavior.always,
              filled: true,
              fillColor: Colors.white,
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: const BorderSide(color: Color(0xFFE8E2FF), width: 1.5),
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: const BorderSide(color: Color(0xFFE8E2FF), width: 1.5),
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: const BorderSide(color: Color(0xFF7B6EFF), width: 1.5),
              ),
              contentPadding: const EdgeInsets.fromLTRB(14, 20, 14, 12),
            ),
          ),
          const SizedBox(height: 12),
          CycleDateRow(
            label: 'START DATE',
            date: _startDate,
            onTap: () => _pickDate(isStart: true),
          ),
          const SizedBox(height: 12),
          CycleDateRow(
            label: 'END DATE',
            date: _endDate,
            onTap: () => _pickDate(isStart: false),
          ),
          const SizedBox(height: 12),
          CycleTimeRow(
            label: 'REVIEW TIME',
            hour: _reviewHour,
            minute: _reviewMinute,
            onTap: _pickTime,
          ),
          const SizedBox(height: 8),
          Text(
            'Connie will check in with you at this time on the last day.',
            style: GoogleFonts.nunito(
              fontSize: 11,
              fontWeight: FontWeight.w600,
              color: const Color(0xFFAAAAC0),
            ),
          ),
          if (_error != null) ...[
            const SizedBox(height: 12),
            Text(
              _error!,
              style: GoogleFonts.nunito(
                fontSize: 12,
                fontWeight: FontWeight.w700,
                color: const Color(0xFFFF6B6B),
              ),
            ),
          ],
          const SizedBox(height: 28),
          GestureDetector(
            onTap: _loading ? null : _create,
            child: AnimatedContainer(
              duration: const Duration(milliseconds: 150),
              width: double.infinity,
              padding: const EdgeInsets.symmetric(vertical: 14),
              decoration: BoxDecoration(
                gradient: _loading
                    ? null
                    : const LinearGradient(
                        colors: [Color(0xFF7B6EFF), Color(0xFFB57BFF)],
                      ),
                color: _loading ? const Color(0xFFE8E2FF) : null,
                borderRadius: BorderRadius.circular(14),
              ),
              child: Center(
                child: _loading
                    ? const SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(
                          color: Color(0xFF7B6EFF),
                          strokeWidth: 2,
                        ),
                      )
                    : Text(
                        'Start Cycle',
                        style: GoogleFonts.nunito(
                          fontSize: 15,
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
