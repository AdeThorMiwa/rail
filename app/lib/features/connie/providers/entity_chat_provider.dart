import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../data/chat_repository.dart';
import '../data/models/block_models.dart';
import '../data/models/chat_models.dart';
import 'connie_provider.dart';

class EntityChatContext {
  final String entityType;
  final String entityId;

  const EntityChatContext({required this.entityType, required this.entityId});

  @override
  bool operator ==(Object other) =>
      other is EntityChatContext &&
      other.entityType == entityType &&
      other.entityId == entityId;

  @override
  int get hashCode => Object.hash(entityType, entityId);
}

class EntityChatState {
  final List<Message> messages;
  final bool isFetching;
  final bool isThinking;
  final String? error;
  final Message? replyingTo;

  const EntityChatState({
    required this.messages,
    this.isFetching = false,
    this.isThinking = false,
    this.error,
    this.replyingTo,
  });

  EntityChatState copyWith({
    List<Message>? messages,
    bool? isFetching,
    bool? isThinking,
    String? error,
    bool clearError = false,
    Message? replyingTo,
    bool clearReply = false,
  }) => EntityChatState(
    messages: messages ?? this.messages,
    isFetching: isFetching ?? this.isFetching,
    isThinking: isThinking ?? this.isThinking,
    error: clearError ? null : (error ?? this.error),
    replyingTo: clearReply ? null : (replyingTo ?? this.replyingTo),
  );
}

class EntityChatNotifier extends FamilyNotifier<EntityChatState, EntityChatContext> {
  ChatRepository get _repository => ref.read(chatRepositoryProvider);

  @override
  EntityChatState build(EntityChatContext arg) {
    _fetch();
    return const EntityChatState(messages: [], isFetching: true);
  }

  Future<void> _fetch() async {
    try {
      final messages = await _repository.getEntityMessages(arg.entityType, arg.entityId);
      state = state.copyWith(messages: messages, isFetching: false, clearError: true);
    } catch (e) {
      state = state.copyWith(isFetching: false, error: e.toString());
    }
  }

  Future<void> send(String text, {String? replyToId}) async {
    final trimmed = text.trim();
    if (trimmed.isEmpty) return;
    final provisional = _provisionalMessage(trimmed, replyToId);
    state = state.copyWith(
      messages: [...state.messages, provisional],
      isThinking: true,
      clearReply: true,
    );
    try {
      await _repository.sendEntityMessage(
        arg.entityType,
        arg.entityId,
        trimmed,
        replyToId: replyToId,
      );
      // Response arrives via SSE → onMessageAdded()
    } catch (e) {
      state = state.copyWith(
        messages: state.messages.where((m) => m.id != provisional.id).toList(),
        isThinking: false,
        error: e.toString(),
      );
    }
  }

  void setReply(Message msg) => state = state.copyWith(replyingTo: msg);
  void clearReply() => state = state.copyWith(clearReply: true);

  void onMessageUpdated(Message msg) {
    state = state.copyWith(
      messages: state.messages.map((m) => m.id == msg.id ? msg : m).toList(),
    );
  }

  void onMessageAdded(Message msg) {
    List<Message> base = state.messages;
    if (msg.isFromUser) {
      base = base.where((m) => !m.id.startsWith('_p_')).toList();
    }
    if (base.any((m) => m.id == msg.id)) return;
    final updated = [...base, msg]
      ..sort((a, b) => a.createdAt.compareTo(b.createdAt));
    state = state.copyWith(
      messages: updated,
      isThinking: msg.isFromUser ? state.isThinking : false,
    );
  }

  Message _provisionalMessage(String text, String? replyToId) {
    return Message(
      id: '_p_${DateTime.now().millisecondsSinceEpoch}',
      conversationId: '',
      sender: MessageSender.user,
      blocks: [
        TextBlock(
          id: '',
          content: TextContent(spans: [ConnieTextSpan(text: text)]),
        ),
      ],
      variant: 'default',
      createdAt: DateTime.now(),
      replyToId: replyToId,
    );
  }

  Future<void> refresh() => _fetch();
}

final entityChatProvider =
    NotifierProviderFamily<EntityChatNotifier, EntityChatState, EntityChatContext>(
  EntityChatNotifier.new,
);
