import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_timezone/flutter_timezone.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../../core/notifications/alarm_scheduler.dart';
import '../../../core/widgets/responsive_wrapper.dart';
import '../../auth/providers/auth_provider.dart';
import '../data/models/scheduling_models.dart';
import '../providers/scheduling_provider.dart';
import '../widgets/days_row.dart';
import '../widgets/energy_row.dart';
import '../widgets/scheduling_header.dart';
import '../widgets/scheduling_section.dart';
import '../widgets/scheduling_submit_button.dart';
import '../widgets/time_range_row.dart';
import '../widgets/timezone_picker.dart';

class SchedulingProfileScreen extends ConsumerStatefulWidget {
  const SchedulingProfileScreen({super.key});

  @override
  ConsumerState<SchedulingProfileScreen> createState() =>
      _SchedulingProfileScreenState();
}

class _SchedulingProfileScreenState
    extends ConsumerState<SchedulingProfileScreen> {
  EnergyPattern _energy = EnergyPattern.morningPeak;
  TimeOfDay _workStart = const TimeOfDay(hour: 9, minute: 0);
  TimeOfDay _workEnd = const TimeOfDay(hour: 13, minute: 0);
  TimeOfDay _wakeTime = const TimeOfDay(hour: 7, minute: 0);
  TimeOfDay _sleepTime = const TimeOfDay(hour: 22, minute: 0);
  String _timezone = 'UTC';
  final Set<int> _selectedDays = {1, 2, 3, 4, 5};
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    FlutterTimezone.getLocalTimezone().then((tz) {
      if (mounted) setState(() => _timezone = tz);
    });
  }

  bool _timeIsBefore(TimeOfDay a, TimeOfDay b) =>
      a.hour < b.hour || (a.hour == b.hour && a.minute < b.minute);

  Future<void> _submit() async {
    if (_selectedDays.isEmpty) {
      _showSnack('Pick at least one work day.');
      return;
    }
    if (!_timeIsBefore(_wakeTime, _sleepTime)) {
      _showSnack('Wake time must be before sleep time.');
      return;
    }
    if (!_timeIsBefore(_workStart, _workEnd)) {
      _showSnack('Deep work start must be before deep work end.');
      return;
    }
    setState(() => _isLoading = true);
    try {
      await ref
          .read(schedulingRepositoryProvider)
          .createProfile(
            deepWorkStart: _workStart,
            deepWorkEnd: _workEnd,
            energyPattern: _energy,
            wakeTime: _wakeTime,
            sleepTime: _sleepTime,
            timezone: _timezone,
            selectedWeekdays: _selectedDays.toList()..sort(),
          );
      await AlarmScheduler.requestPermissions();
      await AlarmScheduler.scheduleWakeAlarm(_wakeTime);
      ref.read(authProvider.notifier).onboardingCompleted();
    } catch (e) {
      if (mounted) {
        setState(() => _isLoading = false);
        _showSnack(_errorMessage(e));
      }
    }
  }

  String _errorMessage(Object e) {
    if (e is DioException) {
      final data = e.response?.data;
      final msg = data is Map ? data['message'] as String? : null;
      if (msg != null) return msg;
    }
    return 'Something went wrong. Please try again.';
  }

  void _showSnack(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          message,
          style: GoogleFonts.nunito(fontWeight: FontWeight.w600),
        ),
        backgroundColor: const Color(0xFF1A1A2E),
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return ResponsiveWrapper(
      child: Scaffold(
        backgroundColor: const Color(0xFFF4F8FF),
        body: SafeArea(
          child: SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(24, 0, 24, 40),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SchedulingHeader(),
                SchedulingSection(
                  label: 'When do you do your best work?',
                  child: EnergyRow(
                    selected: _energy,
                    onChanged: (e) => setState(() => _energy = e),
                  ),
                ),
                SchedulingSection(
                  label: 'Wake & sleep times',
                  sublabel: 'Rail will ring an alarm at wake time every day',
                  child: TimeRangeRow(
                    start: _wakeTime,
                    end: _sleepTime,
                    onStartChanged: (t) => setState(() => _wakeTime = t),
                    onEndChanged: (t) => setState(() => _sleepTime = t),
                  ),
                ),
                SchedulingSection(
                  label: 'Deep work window',
                  sublabel: 'When should I block your focused time?',
                  child: TimeRangeRow(
                    start: _workStart,
                    end: _workEnd,
                    onStartChanged: (t) => setState(() => _workStart = t),
                    onEndChanged: (t) => setState(() => _workEnd = t),
                  ),
                ),

                SchedulingSection(
                  label: 'Your timezone',
                  sublabel: 'Auto-detected — change if incorrect',
                  child: TimezonePicker(
                    value: _timezone,
                    onChanged: (tz) => setState(() => _timezone = tz),
                  ),
                ),
                SchedulingSection(
                  label: 'Your work days',
                  child: DaysRow(
                    selected: _selectedDays,
                    onToggle: (d) => setState(() {
                      if (_selectedDays.contains(d)) {
                        _selectedDays.remove(d);
                      } else {
                        _selectedDays.add(d);
                      }
                    }),
                  ),
                ),
                const SizedBox(height: 32),
                SchedulingSubmitButton(
                  isLoading: _isLoading,
                  onTap: _isLoading ? null : _submit,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
