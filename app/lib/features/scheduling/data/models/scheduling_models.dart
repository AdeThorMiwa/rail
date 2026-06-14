import 'package:flutter/material.dart';

enum EnergyPattern { morningPeak, eveningPeak, consistent }

extension EnergyPatternX on EnergyPattern {
  String get apiValue => switch (this) {
        EnergyPattern.morningPeak => 'MORNING_PEAK',
        EnergyPattern.eveningPeak => 'EVENING_PEAK',
        EnergyPattern.consistent => 'CONSISTENT',
      };

  String get label => switch (this) {
        EnergyPattern.morningPeak => 'Morning',
        EnergyPattern.eveningPeak => 'Evening',
        EnergyPattern.consistent => 'Flexible',
      };

  String get emoji => switch (this) {
        EnergyPattern.morningPeak => '🌅',
        EnergyPattern.eveningPeak => '🌙',
        EnergyPattern.consistent => '⚡',
      };

  String get description => switch (this) {
        EnergyPattern.morningPeak => 'Peak before noon',
        EnergyPattern.eveningPeak => 'Peak after 6pm',
        EnergyPattern.consistent => 'Steady all day',
      };
}

class SchedulingDayModel {
  final int weekday; // 1 = Monday … 7 = Sunday (matches DateTime.weekday)

  const SchedulingDayModel(this.weekday);

  String get apiValue => const {
        1: 'MONDAY',
        2: 'TUESDAY',
        3: 'WEDNESDAY',
        4: 'THURSDAY',
        5: 'FRIDAY',
        6: 'SATURDAY',
        7: 'SUNDAY',
      }[weekday]!;

  String get label => const {
        1: 'M',
        2: 'T',
        3: 'W',
        4: 'T',
        5: 'F',
        6: 'S',
        7: 'S',
      }[weekday]!;
}

class SchedulingProfileResponse {
  final String pid;
  final TimeOfDay deepWorkStart;
  final TimeOfDay deepWorkEnd;
  final TimeOfDay wakeTime;
  final TimeOfDay sleepTime;
  final EnergyPattern energyPattern;
  final String timezone;

  const SchedulingProfileResponse({
    required this.pid,
    required this.deepWorkStart,
    required this.deepWorkEnd,
    required this.wakeTime,
    required this.sleepTime,
    required this.energyPattern,
    required this.timezone,
  });

  factory SchedulingProfileResponse.fromJson(Map<String, dynamic> json) {
    return SchedulingProfileResponse(
      pid: json['pid'] as String,
      deepWorkStart: _parseTime(json['deepWorkStart'] as String),
      deepWorkEnd: _parseTime(json['deepWorkEnd'] as String),
      wakeTime: _parseTime(json['wakeTime'] as String? ?? '07:00'),
      sleepTime: _parseTime(json['sleepTime'] as String? ?? '22:00'),
      energyPattern: _parseEnergyPattern(json['energyPattern'] as String),
      timezone: json['timezone'] as String? ?? 'UTC',
    );
  }

  static TimeOfDay _parseTime(String s) {
    final parts = s.split(':');
    return TimeOfDay(hour: int.parse(parts[0]), minute: int.parse(parts[1]));
  }

  static EnergyPattern _parseEnergyPattern(String s) => switch (s) {
        'MORNING_PEAK' => EnergyPattern.morningPeak,
        'EVENING_PEAK' => EnergyPattern.eveningPeak,
        _ => EnergyPattern.consistent,
      };
}
