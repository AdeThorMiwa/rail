import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../auth/providers/auth_provider.dart';
import '../data/goals_repository.dart';
import '../data/models/goal_models.dart';

final goalsRepositoryProvider = Provider<GoalsRepository>((ref) {
  return GoalsRepository(ref.watch(apiClientProvider));
});

final goalsProvider = AsyncNotifierProvider<GoalsNotifier, List<GoalListItem>>(
  GoalsNotifier.new,
);

class GoalsNotifier extends AsyncNotifier<List<GoalListItem>> {
  @override
  Future<List<GoalListItem>> build() {
    return ref.watch(goalsRepositoryProvider).list();
  }

  Future<void> confirmProposal(String proposalId) async {
    final repo = ref.read(goalsRepositoryProvider);
    await repo.confirmProposal(proposalId);
  }
}

final goalDetailProvider = FutureProvider.family<GoalDetail, String>((
  ref,
  goalPid,
) {
  return ref.watch(goalsRepositoryProvider).getDetail(goalPid);
});
