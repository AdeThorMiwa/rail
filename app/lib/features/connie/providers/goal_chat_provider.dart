import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../data/chat_repository.dart';
import '../data/models/chat_models.dart';
import 'connie_provider.dart';
import 'entity_chat_provider.dart';

class GoalChatState {
  final List<Message> activity;
  final bool isFetching;
  final bool isThinking;
  final String? error;
  final Message? replyingTo;

  const GoalChatState({
    required this.activity,
    this.isFetching = false,
    this.isThinking = false,
    this.error,
    this.replyingTo,
  });

  GoalChatState copyWith({
    List<Message>? activity,
    bool? isFetching,
    bool? isThinking,
    String? error,
    bool clearError = false,
    Message? replyingTo,
    bool clearReply = false,
  }) => GoalChatState(
    activity: activity ?? this.activity,
    isFetching: isFetching ?? this.isFetching,
    isThinking: isThinking ?? this.isThinking,
    error: clearError ? null : (error ?? this.error),
    replyingTo: clearReply ? null : (replyingTo ?? this.replyingTo),
  );
}

class GoalChatNotifier extends FamilyNotifier<GoalChatState, String> {
  ChatRepository get _repository => ref.read(chatRepositoryProvider);

  EntityChatContext get _goalCtx =>
      EntityChatContext(entityType: 'GOAL', entityId: arg);

  @override
  GoalChatState build(String goalPid) {
    _fetch();

    // Pick up live goal messages pushed via SSE → entityChatProvider
    ref.listen(entityChatProvider(_goalCtx), (prev, next) {
      _mergeMessages(next.messages);
      if (next.isThinking != (prev?.isThinking ?? false)) {
        state = state.copyWith(isThinking: next.isThinking);
      }
    });

    return const GoalChatState(activity: [], isFetching: true);
  }

  Future<void> _fetch() async {
    try {
      final messages = await _repository.getGoalActivity(arg);
      state = state.copyWith(activity: messages, isFetching: false, clearError: true);
    } catch (e) {
      state = state.copyWith(isFetching: false, error: e.toString());
    }
  }

  void _mergeMessages(List<Message> incoming) {
    final existing = {for (final m in state.activity) m.id};
    final newMessages = incoming.where((m) => !existing.contains(m.id)).toList();
    if (newMessages.isEmpty) return;
    final merged = [...state.activity, ...newMessages]
      ..sort((a, b) => a.createdAt.compareTo(b.createdAt));
    state = state.copyWith(activity: merged, isThinking: false);
  }

  Future<void> send(String text, {String? replyToId}) async {
    if (text.trim().isEmpty) return;
    state = state.copyWith(isThinking: true, clearReply: true);
    try {
      await _repository.sendEntityMessage('GOAL', arg, text.trim(),
          replyToId: replyToId);
      // Response arrives via SSE → entityChatProvider → listener above
    } catch (e) {
      state = state.copyWith(isThinking: false, error: e.toString());
    }
  }

  void setReply(Message msg) => state = state.copyWith(replyingTo: msg);
  void clearReply() => state = state.copyWith(clearReply: true);

  Future<void> refresh() => _fetch();
}

final goalChatProvider =
    NotifierProviderFamily<GoalChatNotifier, GoalChatState, String>(
  GoalChatNotifier.new,
);
