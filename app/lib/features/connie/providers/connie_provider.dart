import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:rail/features/connie/data/chat_repository.dart';
import 'package:rail/features/connie/data/models/block_models.dart';
import 'package:rail/features/connie/data/models/chat_models.dart';
import '../../../core/events/sse_event.dart';
import '../../../core/events/sse_event_bus.dart';
import '../../auth/providers/auth_provider.dart';
import 'entity_chat_provider.dart';

final chatRepositoryProvider = Provider<ChatRepository>((ref) {
  return ChatRepository(ref.watch(apiClientProvider));
});

class ConnieState {
  final List<Message> messages;
  final bool isLoading;
  final String? error;
  final Message? replyingTo;

  const ConnieState({
    required this.messages,
    this.isLoading = false,
    this.error,
    this.replyingTo,
  });

  ConnieState copyWith({
    List<Message>? messages,
    bool? isLoading,
    String? error,
    Message? replyingTo,
    bool clearReply = false,
    bool clearError = false,
  }) => ConnieState(
    messages: messages ?? this.messages,
    isLoading: isLoading ?? this.isLoading,
    error: clearError ? null : error,
    replyingTo: clearReply ? null : (replyingTo ?? this.replyingTo),
  );
}

// Not autoDispose — must survive while a send() is in-flight.
final connieProvider = NotifierProvider<ConnieNotifier, ConnieState>(
  ConnieNotifier.new,
);

class ConnieNotifier extends Notifier<ConnieState> {
  ChatRepository get _repository => ref.read(chatRepositoryProvider);
  StreamSubscription<SseEvent>? _sub;

  @override
  ConnieState build() {
    _fetchMessages();
    _sub = ref.read(sseEventBusProvider).stream.listen(_handleSseEvent);
    ref.onDispose(() => _sub?.cancel());
    return ConnieState(messages: [], isLoading: true);
  }

  Future<void> _fetchMessages() async {
    try {
      final messages = await _repository.getMessages();
      state = state.copyWith(messages: messages, isLoading: false, clearError: true);
    } catch (e) {
      state = state.copyWith(isLoading: false, error: e.toString());
    }
  }

  void _handleSseEvent(SseEvent event) {
    switch (event.type) {
      case 'thinking_start':
        state = state.copyWith(isLoading: true);

      case 'thinking_stop':
        state = state.copyWith(isLoading: false);

      case 'message_added':
        final msg = Message.fromJson(event.data);
        if (msg.chatEntityType != 'GLOBAL' && msg.chatEntityId != null) {
          ref.read(entityChatProvider(EntityChatContext(
            entityType: msg.chatEntityType,
            entityId: msg.chatEntityId!,
          )).notifier).onMessageAdded(msg);
          return;
        }
        if (state.messages.any((m) => m.id == msg.id)) return;
        final updated = msg.isFromUser
            ? [...state.messages.where((m) => !m.id.startsWith('_p_')), msg]
            : [...state.messages, msg];
        // Sort real messages by createdAt to handle out-of-order SSE delivery
        // (SsePushListener is @Async so events can race). Provisionals stay last.
        updated.sort((a, b) {
          if (a.id.startsWith('_p_')) return 1;
          if (b.id.startsWith('_p_')) return -1;
          return a.createdAt.compareTo(b.createdAt);
        });
        state = state.copyWith(
          messages: updated,
          isLoading: msg.isFromUser ? state.isLoading : false,
        );

      case 'message_updated':
        final updated = Message.fromJson(event.data);
        if (updated.chatEntityType != 'GLOBAL' && updated.chatEntityId != null) {
          ref.read(entityChatProvider(EntityChatContext(
            entityType: updated.chatEntityType,
            entityId: updated.chatEntityId!,
          )).notifier).onMessageUpdated(updated);
          return;
        }
        state = state.copyWith(
          messages: state.messages.map((m) => m.id == updated.id ? updated : m).toList(),
        );

      case 'error':
        state = state.copyWith(
          messages: state.messages.where((m) => !m.id.startsWith('_p_')).toList(),
          isLoading: false,
          error: event.data['message'] as String? ?? 'Something went wrong',
        );
    }
  }

  Future<void> send(String text) async {
    if (text.trim().isEmpty) return;
    final trimmed = text.trim();
    final replyToId = state.replyingTo?.id;
    final provisional = _provisionalMessage(trimmed, replyToId);
    state = state.copyWith(
      messages: [...state.messages, provisional],
      clearReply: true,
    );
    try {
      await _repository.send(trimmed, replyToId: replyToId);
    } catch (e) {
      state = state.copyWith(
        messages: state.messages.where((m) => m.id != provisional.id).toList(),
        isLoading: false,
        error: e.toString(),
      );
    }
  }

  void setReply(Message message) => state = state.copyWith(replyingTo: message);
  void clearReply() => state = state.copyWith(clearReply: true);
  Future<void> refresh() => _fetchMessages();
  void reset() => state = build();

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
}
