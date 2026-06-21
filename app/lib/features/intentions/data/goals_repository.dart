import '../../../core/network/api_client.dart';
import 'models/goal_models.dart';

class GoalsRepository {
  final ApiClient _client;

  const GoalsRepository(this._client);

  Future<List<TaskDetail>> listTasks({String? goalId, String? milestoneId}) async {
    final res = await _client.dio.get(
      '/tasks',
      queryParameters: {
        'goalId': goalId,
        'milestoneId': milestoneId,
      },
    );
    return (res.data as List)
        .map((e) => TaskDetail.fromJson(e as Map<String, dynamic>))
        .toList();
  }

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

  Future<void> completeGoal(String goalPid, {String? notes}) async {
    await _client.dio.post(
      '/goals/$goalPid/complete',
      data: notes != null && notes.isNotEmpty ? {'notes': notes} : null,
    );
  }
}
