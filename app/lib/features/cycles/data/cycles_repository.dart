import '../../../core/network/api_client.dart';
import 'models/cycle_models.dart';

class CyclesRepository {
  final ApiClient _client;

  const CyclesRepository(this._client);

  Future<UserCycle?> getActive() async {
    final res = await _client.dio.get('/cycles/active');
    if (res.statusCode == 204 || res.data == null) return null;
    return UserCycle.fromJson(res.data as Map<String, dynamic>);
  }

  Future<UserCycle> create({
    required DateTime startDate,
    required DateTime endDate,
    required int reviewHour,
    required int reviewMinute,
    String? title,
  }) async {
    final body = <String, dynamic>{
      'startDate': _fmtDate(startDate),
      'endDate': _fmtDate(endDate),
      'reviewTime': '${reviewHour.toString().padLeft(2, '0')}:${reviewMinute.toString().padLeft(2, '0')}',
    };
    if (title != null && title.trim().isNotEmpty) body['title'] = title.trim();
    final res = await _client.dio.post('/cycles', data: body);
    return UserCycle.fromJson(res.data as Map<String, dynamic>);
  }

  Future<List<CycleFocusGoal>> getFocuses(String cyclePid) async {
    final res = await _client.dio.get('/cycles/$cyclePid/focuses');
    final list = res.data as List<dynamic>;
    return list
        .map((e) => CycleFocusGoal.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  static String _fmtDate(DateTime d) =>
      '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';
}
