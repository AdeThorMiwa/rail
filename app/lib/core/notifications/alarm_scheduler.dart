import 'package:alarm/alarm.dart';
import 'package:flutter/material.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';

import '../../features/home/data/models/schedule_models.dart';

class AlarmScheduler {
  static bool _initialized = false;
  static final _fln = FlutterLocalNotificationsPlugin();

  static const int _wakeAlarmId = 1000;
  static const int _taskAlarmBase = 1;
  static const int maxRingSeconds = 300;
  static const int wakeRingSeconds = 300;
  static const String _audioPath = 'assets/audio/alarm.wav';

  static Future<void> init() async {
    if (_initialized) return;
    await Alarm.init(showDebugLogs: false);
    const android = AndroidInitializationSettings('@mipmap/ic_launcher');
    const ios = DarwinInitializationSettings(
      requestAlertPermission: false,
      requestBadgePermission: false,
      requestSoundPermission: false,
    );
    await _fln.initialize(const InitializationSettings(android: android, iOS: ios));
    _initialized = true;
  }

  static Future<void> requestPermissions() async {
    final android = _fln.resolvePlatformSpecificImplementation<
        AndroidFlutterLocalNotificationsPlugin>();
    if (android != null) {
      await android.requestNotificationsPermission();
      await android.requestExactAlarmsPermission();
      await android.requestFullScreenIntentPermission();
    }
    final ios = _fln.resolvePlatformSpecificImplementation<
        IOSFlutterLocalNotificationsPlugin>();
    await ios?.requestPermissions(alert: true, badge: true, sound: true);
  }

  /// Prompts for any alarm permissions that were added after the user's initial
  /// onboarding (e.g. USE_FULL_SCREEN_INTENT on Android 14+). Safe to call on
  /// every app launch — the plugin checks internally and only opens Settings if
  /// the permission is actually missing.
  static Future<void> requestMissingPermissions() async {
    final android = _fln.resolvePlatformSpecificImplementation<
        AndroidFlutterLocalNotificationsPlugin>();
    if (android == null) return;
    await android.requestFullScreenIntentPermission();
  }

  /// Schedules a single alarm. All alarm scheduling goes through here.
  static Future<void> scheduleAlarm({
    required int id,
    required DateTime dateTime,
    required String title,
    required String body,
    bool warningNotificationOnKill = true,
  }) async {
    await Alarm.set(
      alarmSettings: AlarmSettings(
        id: id,
        dateTime: dateTime,
        assetAudioPath: _audioPath,
        loopAudio: true,
        vibrate: false,
        volume: 0.8,
        fadeDuration: 0,
        warningNotificationOnKill: warningNotificationOnKill,
        androidFullScreenIntent: true,
        notificationSettings: NotificationSettings(
          title: title,
          body: body,
          stopButton: 'Dismiss',
        ),
      ),
    );
  }

  static Future<void> scheduleWakeAlarm(TimeOfDay wakeTime) async {
    await Alarm.stop(_wakeAlarmId);

    final now = DateTime.now();
    DateTime scheduled = DateTime(
      now.year, now.month, now.day,
      wakeTime.hour, wakeTime.minute,
    );
    if (scheduled.isBefore(now)) {
      scheduled = scheduled.add(const Duration(days: 1));
    }

    await scheduleAlarm(
      id: _wakeAlarmId,
      dateTime: scheduled,
      title: 'Good morning ☀️',
      body: 'Your schedule is ready — time to check in.',
    );
  }

  static Future<void> rescheduleTaskAlarms(DailySchedule schedule) async {
    await cancelTaskAlarms();

    final now = DateTime.now();
    int id = _taskAlarmBase;

    for (final entry in schedule.taskEntries) {
      if (!entry.isPending) { id++; continue; }
      final task = entry.task;
      if (task == null) { id++; continue; }

      final alarmTime = DateTime(
        now.year, now.month, now.day,
        entry.startTime.hour, entry.startTime.minute,
      );
      if (alarmTime.isBefore(now)) { id++; continue; }

      await scheduleAlarm(
        id: id,
        dateTime: alarmTime,
        title: 'Starting now',
        body: task.title,
      );
      id++;
    }
  }

  static Future<void> cancelTaskAlarms() async {
    final all = await Alarm.getAlarms();
    for (final a in all) {
      if (a.id != _wakeAlarmId) {
        await Alarm.stop(a.id);
      }
    }
  }

  static int ringSecondsForEntry(ScheduleEntry entry) {
    final dur = entry.durationMinutes;
    if (dur <= 0) return maxRingSeconds;
    return (dur * 60).clamp(0, maxRingSeconds);
  }
}
