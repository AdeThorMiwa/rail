import 'package:alarm/alarm.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import 'core/events/sse_event_bus.dart';
import 'core/notifications/alarm_scheduler.dart';
import 'core/notifications/notifications_provider.dart';
import 'core/router/app_router.dart';
import 'features/auth/providers/auth_provider.dart';
import 'features/connie/providers/connie_provider.dart';
import 'features/connie/providers/entity_chat_provider.dart';
import 'features/connie/providers/goal_chat_provider.dart';
import 'features/cycles/providers/cycle_provider.dart';
import 'features/home/providers/home_provider.dart';
import 'features/home/widgets/alarm_screen.dart';
import 'features/intentions/providers/goals_provider.dart';

final _alarmRingProvider = StreamProvider<AlarmSettings>((ref) {
  return Alarm.ringStream.stream;
});

final activeAlarmProvider = StateProvider<AlarmSettings?>((_) => null);

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await AlarmScheduler.init();
  runApp(const ProviderScope(child: RailApp()));
}

class RailApp extends ConsumerWidget {
  const RailApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final authState = ref.watch(authProvider);
    ref.watch(sseConnectionManagerProvider);

    ref.listen(_alarmRingProvider, (_, next) {
      next.whenData((settings) {
        ref.read(activeAlarmProvider.notifier).state = settings;
      });
    });

    ref.listen(authProvider, (prev, next) {
      final wasAuthenticated = prev?.valueOrNull is AuthAuthenticated;
      final isNowUnauthenticated = next.valueOrNull is AuthUnauthenticated;
      if (wasAuthenticated && isNowUnauthenticated) {
        ref.invalidate(connieProvider);
        ref.invalidate(entityChatProvider);
        ref.invalidate(goalChatProvider);
        ref.invalidate(goalsProvider);
        ref.invalidate(goalDetailProvider);
        ref.invalidate(cycleProvider);
        ref.invalidate(cycleFocusesProvider);
        ref.invalidate(todayScheduleProvider);
        ref.invalidate(schedulingProfileProvider);
        ref.invalidate(notificationsProvider);
      }
    });

    if (authState.isLoading) {
      return const MaterialApp(
        debugShowCheckedModeBanner: false,
        home: Scaffold(
          backgroundColor: Color(0xFFF4F8FF),
          body: Center(
            child: CircularProgressIndicator(color: Color(0xFF7B6EFF)),
          ),
        ),
      );
    }

    final router = ref.watch(goRouterProvider);

    return MaterialApp.router(
      title: 'Rail',
      debugShowCheckedModeBanner: false,
      theme: _buildTheme(),
      routerConfig: router,
      builder: (context, child) {
        return Consumer(
          builder: (context, ref, _) {
            final activeAlarm = ref.watch(activeAlarmProvider);
            final base = child ?? const SizedBox.shrink();

            if (activeAlarm == null) return base;

            // Find the matching pending entry to get ring duration
            // We store ring duration alongside the alarm settings via a lookup
            final ringSeconds = _ringSecondsFor(activeAlarm.id, ref);

            return Stack(
              children: [
                base,
                Positioned.fill(
                  child: AlarmScreen(
                    alarmSettings: activeAlarm,
                    ringSeconds: ringSeconds,
                    onDismiss: () async {
                      await Alarm.stop(activeAlarm.id);
                      ref.read(activeAlarmProvider.notifier).state = null;
                    },
                  ),
                ),
              ],
            );
          },
        );
      },
    );
  }

  int _ringSecondsFor(int alarmId, WidgetRef ref) {
    if (alarmId == 1000) return AlarmScheduler.wakeRingSeconds;

    final schedule = ref.read(todayScheduleProvider).valueOrNull;
    if (schedule == null) return AlarmScheduler.maxRingSeconds;

    final entries = schedule.taskEntries;
    final index = alarmId - 1; // taskAlarmBase = 1
    if (index >= 0 && index < entries.length) {
      return AlarmScheduler.ringSecondsForEntry(entries[index]);
    }
    return AlarmScheduler.maxRingSeconds;
  }

  ThemeData _buildTheme() {
    final base = ThemeData(
      brightness: Brightness.light,
      scaffoldBackgroundColor: const Color(0xFFF4F8FF),
      colorScheme: const ColorScheme.light(
        primary: Color(0xFF7B6EFF),
        surface: Colors.white,
      ),
      useMaterial3: true,
    );
    return base.copyWith(
      textTheme: GoogleFonts.nunitoTextTheme(base.textTheme),
    );
  }
}
