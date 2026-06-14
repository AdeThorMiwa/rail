import 'package:flutter/material.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:timezone/data/latest_all.dart' as tz;
import 'package:timezone/timezone.dart' as tz;

import '../../features/home/data/models/schedule_models.dart';

class AlarmScheduler {
  static final _plugin = FlutterLocalNotificationsPlugin();
  static bool _initialized = false;

  static const _channelId = 'rail_alarms';
  static const _channelName = 'Rail Alarms';
  static const _wakeAlarmId = 0;

  static Future<void> init() async {
    if (_initialized) return;
    tz.initializeTimeZones();
    const android = AndroidInitializationSettings('@mipmap/ic_launcher');
    const ios = DarwinInitializationSettings(
      requestAlertPermission: false,
      requestBadgePermission: false,
      requestSoundPermission: false,
    );
    await _plugin.initialize(
      const InitializationSettings(android: android, iOS: ios),
    );
    _initialized = true;
  }

  static Future<void> requestPermissions() async {
    final android = _plugin.resolvePlatformSpecificImplementation<
        AndroidFlutterLocalNotificationsPlugin>();
    if (android != null) {
      await android.requestNotificationsPermission();
      await android.requestExactAlarmsPermission();
    }
    final ios = _plugin.resolvePlatformSpecificImplementation<
        IOSFlutterLocalNotificationsPlugin>();
    await ios?.requestPermissions(alert: true, badge: true, sound: true);
  }

  static Future<void> scheduleWakeAlarm(TimeOfDay wakeTime) async {
    await _plugin.cancel(_wakeAlarmId);

    final now = tz.TZDateTime.now(tz.local);
    var scheduled = tz.TZDateTime(
      tz.local,
      now.year,
      now.month,
      now.day,
      wakeTime.hour,
      wakeTime.minute,
    );
    if (scheduled.isBefore(now)) {
      scheduled = scheduled.add(const Duration(days: 1));
    }

    await _plugin.zonedSchedule(
      _wakeAlarmId,
      'Good morning! ☀️',
      'Time to check in — your schedule is ready.',
      scheduled,
      NotificationDetails(
        android: AndroidNotificationDetails(
          _channelId,
          _channelName,
          importance: Importance.max,
          priority: Priority.max,
          fullScreenIntent: true,
        ),
        iOS: const DarwinNotificationDetails(
          interruptionLevel: InterruptionLevel.timeSensitive,
        ),
      ),
      matchDateTimeComponents: DateTimeComponents.time,
      androidScheduleMode: AndroidScheduleMode.exactAllowWhileIdle,
    );
  }

  static Future<void> rescheduleTaskAlarms(DailySchedule schedule) async {
    await cancelTaskAlarms();

    final now = tz.TZDateTime.now(tz.local);
    int id = 1;

    for (final entry in schedule.taskEntries) {
      if (!entry.isPending) continue;
      final task = entry.task;
      if (task == null) continue;

      final alarmTime = tz.TZDateTime(
        tz.local,
        now.year,
        now.month,
        now.day,
        entry.startTime.hour,
        entry.startTime.minute,
      );

      if (alarmTime.isBefore(now)) {
        id++;
        continue;
      }

      await _plugin.zonedSchedule(
        id,
        'Starting now: ${task.title}',
        null,
        alarmTime,
        NotificationDetails(
          android: AndroidNotificationDetails(
            _channelId,
            _channelName,
            importance: Importance.max,
            priority: Priority.max,
            fullScreenIntent: true,
          ),
          iOS: const DarwinNotificationDetails(
            interruptionLevel: InterruptionLevel.timeSensitive,
          ),
        ),
        androidScheduleMode: AndroidScheduleMode.exactAllowWhileIdle,
      );
      id++;
    }
  }

  static Future<void> cancelTaskAlarms() async {
    final pending = await _plugin.pendingNotificationRequests();
    for (final n in pending) {
      if (n.id != _wakeAlarmId) {
        await _plugin.cancel(n.id);
      }
    }
  }
}
