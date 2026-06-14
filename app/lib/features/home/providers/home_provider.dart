import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/events/sse_event_bus.dart';
import '../../../core/notifications/alarm_scheduler.dart';
import '../../auth/providers/auth_provider.dart';
import '../../scheduling/data/scheduling_repository.dart';
import '../../scheduling/data/models/scheduling_models.dart';
import '../data/home_repository.dart';
import '../data/models/schedule_models.dart';

final homeRepositoryProvider = Provider<HomeRepository>((ref) {
  return HomeRepository(ref.watch(apiClientProvider));
});

final _schedulingRepositoryProvider = Provider<SchedulingRepository>((ref) {
  return SchedulingRepository(ref.watch(apiClientProvider));
});

final schedulingProfileProvider = FutureProvider<SchedulingProfileResponse?>((ref) async {
  final authState = await ref.watch(authProvider.future);
  if (authState is! AuthAuthenticated) return null;
  return ref.read(_schedulingRepositoryProvider).getProfile();
});

final todayScheduleProvider = FutureProvider<DailySchedule?>((ref) async {
  final authState = await ref.watch(authProvider.future);
  if (authState is! AuthAuthenticated) {
    return Completer<DailySchedule?>().future;
  }
  final schedule = await ref.read(homeRepositoryProvider).getTodaySchedule();
  if (schedule != null) {
    AlarmScheduler.rescheduleTaskAlarms(schedule);
  }
  return schedule;
});

// Listens for schedule_updated events from the bus and refreshes todayScheduleProvider.
// HomeScreen watches this to keep it alive while on-screen.
final scheduleEventListenerProvider = NotifierProvider<ScheduleEventListener, void>(
  ScheduleEventListener.new,
);

class ScheduleEventListener extends Notifier<void> {
  @override
  void build() {
    final sub = ref
        .read(sseEventBusProvider)
        .stream
        .where((e) => e.type == 'schedule_updated')
        .listen((_) => ref.invalidate(todayScheduleProvider));
    ref.onDispose(sub.cancel);
  }
}
