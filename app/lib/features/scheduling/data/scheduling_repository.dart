import 'package:flutter/material.dart';
import '../../../core/network/api_client.dart';
import 'models/scheduling_models.dart';

class SchedulingRepository {
  final ApiClient _client;

  const SchedulingRepository(this._client);

  Future<SchedulingProfileResponse?> getProfile() async {
    try {
      final res = await _client.dio.get('/me/scheduling-profile');
      return SchedulingProfileResponse.fromJson(res.data as Map<String, dynamic>);
    } catch (_) {
      return null;
    }
  }

  Future<SchedulingProfileResponse> createProfile({
    required TimeOfDay deepWorkStart,
    required TimeOfDay deepWorkEnd,
    required EnergyPattern energyPattern,
    required TimeOfDay wakeTime,
    required TimeOfDay sleepTime,
    required String timezone,
    required List<int> selectedWeekdays,
  }) async {
    final res = await _client.dio.post('/me/scheduling-profile', data: {
      'deepWorkStart': _formatTime(deepWorkStart),
      'deepWorkEnd': _formatTime(deepWorkEnd),
      'energyPattern': energyPattern.apiValue,
      'wakeTime': _formatTime(wakeTime),
      'sleepTime': _formatTime(sleepTime),
      'timezone': timezone,
      'days': selectedWeekdays
          .map((d) => {'dayOfWeek': SchedulingDayModel(d).apiValue})
          .toList(),
    });
    return SchedulingProfileResponse.fromJson(res.data as Map<String, dynamic>);
  }

  String _formatTime(TimeOfDay t) =>
      '${t.hour.toString().padLeft(2, '0')}:${t.minute.toString().padLeft(2, '0')}:00';
}
