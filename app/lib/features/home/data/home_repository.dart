import 'package:dio/dio.dart';
import '../../../core/network/api_client.dart';
import 'models/schedule_models.dart';

class HomeRepository {
  final ApiClient _client;

  const HomeRepository(this._client);

  Future<DailySchedule?> getTodaySchedule() async {
    try {
      final res = await _client.dio.get('/schedule/today');
      if (res.data == null) return null;
      return DailySchedule.fromJson(res.data as Map<String, dynamic>);
    } on DioException catch (e) {
      if (e.response?.statusCode == 404) return null;
      rethrow;
    }
  }

  Future<ScheduleEntry> completeEntry(
    String entryPid, {
    required String completionType,
    String? completionNote,
    double? actualValue,
  }) async {
    final res = await _client.dio.post('/schedule/entries/$entryPid/complete', data: {
      'completionType': completionType,
      if (completionNote != null && completionNote.isNotEmpty)
        'completionNote': completionNote,
      if (actualValue != null) 'actualValue': actualValue,  // ignore: use_null_aware_elements
    });
    return ScheduleEntry.fromJson(res.data as Map<String, dynamic>);
  }

  Future<ScheduleEntry> skipEntry(String entryPid, {String? reason}) async {
    final res = await _client.dio.post('/schedule/entries/$entryPid/skip', data: {
      if (reason != null && reason.isNotEmpty) 'reason': reason,
    });
    return ScheduleEntry.fromJson(res.data as Map<String, dynamic>);
  }

  Future<ScheduleEntry> slipEntry(String entryPid, {String? note}) async {
    final res = await _client.dio.post('/schedule/entries/$entryPid/slip', data: {
      'note': note,
    });
    return ScheduleEntry.fromJson(res.data as Map<String, dynamic>);
  }
}
