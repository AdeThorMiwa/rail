import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../../core/notifications/notifications_provider.dart';
import '../../../core/notifications/alarm_scheduler.dart';
import '../../../core/widgets/connie_fab.dart';
import '../../auth/providers/auth_provider.dart';
import '../../scheduling/data/models/scheduling_models.dart';
import '../data/models/schedule_models.dart';
import '../providers/home_provider.dart';
import '../widgets/alarm_banner.dart';
import '../widgets/empty_day.dart';
import '../widgets/off_hours_screen.dart';
import '../widgets/schedule_day.dart';
import '../widgets/schedule_generating_card.dart';

class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen>
    with WidgetsBindingObserver {
  Timer? _timer;
  bool _showBanner = false;
  String _bannerTitle = '';

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _timer = Timer.periodic(const Duration(seconds: 30), (_) {
      if (mounted) setState(() {});
    });
    WidgetsBinding.instance.addPostFrameCallback((_) {
      AlarmScheduler.requestMissingPermissions();
    });
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _timer?.cancel();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      ref.invalidate(todayScheduleProvider);
    }
  }

  Future<void> _openConnie() async {
    await context.push('/connie');
    if (mounted) ref.invalidate(todayScheduleProvider);
  }

  void _onTaskActivated(String title) {
    setState(() {
      _showBanner = true;
      _bannerTitle = title;
    });
    Future.delayed(const Duration(seconds: 3), () {
      if (mounted) setState(() => _showBanner = false);
    });
  }

  bool _hasActiveEntry(DailySchedule schedule) {
    final now = TimeOfDay.now();
    return schedule.taskEntries.any(
      (e) => e.isPending && e.timeStateAt(now) == ScheduleEntryTimeState.active,
    );
  }

  @override
  Widget build(BuildContext context) {
    ref.watch(scheduleEventListenerProvider);
    ref.watch(notificationsProvider);
    final scheduleState = ref.watch(todayScheduleProvider);
    final profileState = ref.watch(schedulingProfileProvider);
    final authState = ref.watch(authProvider).valueOrNull;
    final user = authState is AuthAuthenticated ? authState.user : null;
    final now = DateTime.now();
    final firstName = user?.displayName.split(' ').firstOrNull ?? 'there';

    return Scaffold(
      backgroundColor: const Color(0xFFF4F8FF),
      body: Stack(
        children: [
          scheduleState.when(
            loading: () => const Center(
              child: CircularProgressIndicator(color: Color(0xFF7B6EFF)),
            ),
            error: (_, _) => Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    'Could not load today\'s schedule',
                    style: GoogleFonts.nunito(
                      fontWeight: FontWeight.w700,
                      color: const Color(0xFF9090AA),
                    ),
                  ),
                  const SizedBox(height: 12),
                  TextButton(
                    onPressed: () => ref.invalidate(todayScheduleProvider),
                    child: Text(
                      'Retry',
                      style: GoogleFonts.nunito(
                        fontWeight: FontWeight.w800,
                        color: const Color(0xFF7B6EFF),
                      ),
                    ),
                  ),
                ],
              ),
            ),
            data: (schedule) {
              if (schedule == null) {
                final profile = profileState.valueOrNull;
                if (profile != null && _isWithinWakeWindow(now, profile)) {
                  return _withHeader(
                    firstName: firstName,
                    now: now,
                    compact: false,
                    child: EmptyDay(onConnieTapped: _openConnie),
                  );
                }
                return OffHoursScreen(onConnieTapped: _openConnie);
              }

              if (schedule.status == DailyScheduleStatus.generating) {
                return _withHeader(
                  firstName: firstName,
                  now: now,
                  compact: false,
                  child: const ScheduleGeneratingCard(),
                );
              }

              final compact = _hasActiveEntry(schedule);

              return _withHeader(
                firstName: firstName,
                now: now,
                compact: compact,
                child: schedule.taskEntries.isEmpty
                    ? EmptyDay(onConnieTapped: _openConnie)
                    : ScheduleDay(
                        schedule: schedule,
                        onTaskActivated: _onTaskActivated,
                      ),
              );
            },
          ),

          // Alarm banner overlays everything from the top
          if (_showBanner)
            Positioned(
              top: 0,
              left: 0,
              right: 0,
              child: AlarmBanner(
                taskTitle: _bannerTitle,
                onDismiss: () => setState(() => _showBanner = false),
              ),
            ),
        ],
      ),
      floatingActionButton: ConnieFab(onTap: _openConnie),
    );
  }

  Widget _withHeader({
    required String firstName,
    required DateTime now,
    required bool compact,
    required Widget child,
  }) {
    final unread = ref.watch(notificationsProvider).length;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (compact)
          _CompactHeader(firstName: firstName, now: now, unread: unread)
        else
          _FullHeader(firstName: firstName, now: now, unread: unread),
        Expanded(
          child: RefreshIndicator(
            color: const Color(0xFF7B6EFF),
            onRefresh: () async => ref.invalidate(todayScheduleProvider),
            child: child,
          ),
        ),
      ],
    );
  }

  bool _isWithinWakeWindow(DateTime now, SchedulingProfileResponse profile) {
    final nowMins = now.hour * 60 + now.minute;
    final startMins = profile.wakeTime.hour * 60 + profile.wakeTime.minute;
    final endMins = profile.sleepTime.hour * 60 + profile.sleepTime.minute;
    return nowMins >= startMins && nowMins < endMins;
  }
}

class _FullHeader extends StatelessWidget {
  final String firstName;
  final DateTime now;
  final int unread;

  const _FullHeader({
    required this.firstName,
    required this.now,
    required this.unread,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 56, 8, 0),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '${_greeting(now)}, $firstName 👋',
                  style: GoogleFonts.nunito(
                    fontSize: 25,
                    fontWeight: FontWeight.w900,
                    color: const Color(0xFF1A1A2E),
                  ),
                ),
                Text(
                  '${_weekday(now.weekday)} · ${_month(now.month)} ${now.day}',
                  style: GoogleFonts.nunito(
                    fontSize: 14,
                    fontWeight: FontWeight.w700,
                    color: const Color(0xFF9090AA),
                  ),
                ),
              ],
            ),
          ),
          _BellBadge(count: unread),
        ],
      ),
    );
  }
}

class _CompactHeader extends StatelessWidget {
  final String firstName;
  final DateTime now;
  final int unread;

  const _CompactHeader({
    required this.firstName,
    required this.now,
    required this.unread,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 52, 8, 0),
      child: Row(
        children: [
          Expanded(
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.baseline,
              textBaseline: TextBaseline.alphabetic,
              children: [
                Text(
                  '${_greeting(now)}, $firstName 👋',
                  style: GoogleFonts.nunito(
                    fontSize: 15,
                    fontWeight: FontWeight.w900,
                    color: const Color(0xFF1A1A2E),
                  ),
                ),
                const SizedBox(width: 10),
                Text(
                  '${_weekday(now.weekday)} · ${_month(now.month)} ${now.day}',
                  style: GoogleFonts.nunito(
                    fontSize: 12,
                    fontWeight: FontWeight.w700,
                    color: const Color(0xFF9090AA),
                  ),
                ),
              ],
            ),
          ),
          _BellBadge(count: unread),
        ],
      ),
    );
  }
}

String _greeting(DateTime now) {
  final h = now.hour;
  if (h < 12) return 'Good morning';
  if (h < 17) return 'Good afternoon';
  return 'Good evening';
}

String _weekday(int d) =>
    ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'][d - 1];

String _month(int m) => [
  'Jan',
  'Feb',
  'Mar',
  'Apr',
  'May',
  'Jun',
  'Jul',
  'Aug',
  'Sep',
  'Oct',
  'Nov',
  'Dec',
][m - 1];

class _BellBadge extends StatelessWidget {
  final int count;

  const _BellBadge({required this.count});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () => context.push('/notifications'),
      child: Padding(
        padding: const EdgeInsets.only(top: 4, right: 4),
        child: Stack(
          clipBehavior: Clip.none,
          children: [
            const Icon(
              Icons.notifications_outlined,
              color: Color(0xFF1A1A2E),
              size: 26,
            ),
            if (count > 0)
              Positioned(
                top: -4,
                right: -4,
                child: Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 5,
                    vertical: 1,
                  ),
                  decoration: BoxDecoration(
                    gradient: const LinearGradient(
                      colors: [Color(0xFF7B6EFF), Color(0xFFB57BFF)],
                    ),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Text(
                    count > 99 ? '99+' : '$count',
                    style: GoogleFonts.nunito(
                      fontSize: 10,
                      fontWeight: FontWeight.w900,
                      color: Colors.white,
                    ),
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}
