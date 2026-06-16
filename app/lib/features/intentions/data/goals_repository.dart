import '../../../core/network/api_client.dart';
import 'models/goal_models.dart';

class GoalsRepository {
  final ApiClient _client;

  const GoalsRepository(this._client);

  Future<List<GoalListItem>> list() async {
    final res = await _client.dio.get('/goals');
    return (res.data as List)
        .map((e) => GoalListItem.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<GoalDetail> getDetail(String goalPid) async {
    final res = await _client.dio.get('/goals/$goalPid');
    return GoalDetail.fromJson(res.data as Map<String, dynamic>);
  }

  Future<void> confirmProposal(String proposalId) async {
    await _client.dio.post('/intentions', data: {'proposalId': proposalId});
  }

  Future<void> slipGoal(String goalPid, {String? note}) async {
    await _client.dio.post('/goals/$goalPid/slip', data: {'note': note});
  }

  Future<void> completeGoal(String goalPid) async {
    await _client.dio.post('/goals/$goalPid/complete');
  }
}
