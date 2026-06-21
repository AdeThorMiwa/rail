import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/events/sse_event_bus.dart';
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

  Future<void> completeGoal(String goalPid, {String? notes}) async {
    final repo = ref.read(goalsRepositoryProvider);
    await repo.completeGoal(goalPid, notes: notes);
    ref.invalidateSelf();
  }
}

final goalDetailProvider = FutureProvider.family<GoalDetail, String>((
  ref,
  goalPid,
) {
  return ref.watch(goalsRepositoryProvider).getDetail(goalPid);
});

final goalTasksProvider = FutureProvider.family<List<TaskDetail>, String>((
  ref,
  goalPid,
) {
  return ref.watch(goalsRepositoryProvider).listTasks(goalId: goalPid);
});

// Listens for intention_updated SSE events and invalidates goal providers.
// Watch this in any screen that should auto-refresh when a new goal is generated.
final intentionEventListenerProvider =
    NotifierProvider<IntentionEventListener, void>(IntentionEventListener.new);

class IntentionEventListener extends Notifier<void> {
  @override
  void build() {
    final sub = ref
        .read(sseEventBusProvider)
        .stream
        .where((e) => e.type == 'intention_updated')
        .listen((_) {
          ref.invalidate(goalsProvider);
          ref.invalidate(goalDetailProvider);
        });
    ref.onDispose(sub.cancel);
  }
}
