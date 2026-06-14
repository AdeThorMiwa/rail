import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../auth/providers/auth_provider.dart';
import '../data/cycles_repository.dart';
import '../data/models/cycle_models.dart';

final cycleFocusesProvider =
    FutureProvider.family<List<CycleFocusGoal>, String>((ref, cyclePid) {
  return ref.watch(cyclesRepositoryProvider).getFocuses(cyclePid);
});

final cyclesRepositoryProvider = Provider<CyclesRepository>((ref) {
  return CyclesRepository(ref.watch(apiClientProvider));
});

final cycleProvider = AsyncNotifierProvider<CycleNotifier, UserCycle?>(
  CycleNotifier.new,
);

class CycleNotifier extends AsyncNotifier<UserCycle?> {
  @override
  Future<UserCycle?> build() {
    return ref.watch(cyclesRepositoryProvider).getActive();
  }

  Future<UserCycle> create({
    required DateTime startDate,
    required DateTime endDate,
    required int reviewHour,
    required int reviewMinute,
    String? title,
  }) async {
    final repo = ref.read(cyclesRepositoryProvider);
    final cycle = await repo.create(
      startDate: startDate,
      endDate: endDate,
      reviewHour: reviewHour,
      reviewMinute: reviewMinute,
      title: title,
    );
    state = AsyncValue.data(cycle);
    return cycle;
  }
}
