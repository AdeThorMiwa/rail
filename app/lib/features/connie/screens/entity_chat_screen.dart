import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:rail/features/connie/widgets/chat_date_divider.dart';
import 'package:rail/features/connie/widgets/connie_command_sheet.dart';
import '../../../../core/notifications/notifications_provider.dart';
import '../data/models/chat_models.dart';
import '../providers/entity_chat_provider.dart';
import '../widgets/connie_header.dart';
import '../widgets/connie_input_bar.dart';
import '../widgets/connie_loading_bubble.dart';
import '../widgets/message_bubble.dart';

class EntityChatScreen extends ConsumerStatefulWidget {
  final String entityType;
  final String entityId;
  final String title;

  const EntityChatScreen({
    super.key,
    required this.entityType,
    required this.entityId,
    required this.title,
  });

  @override
  ConsumerState<EntityChatScreen> createState() => _EntityChatScreenState();
}

class _EntityChatScreenState extends ConsumerState<EntityChatScreen> {
  final _scrollController = ScrollController();

  EntityChatContext get _ctx => EntityChatContext(
    entityType: widget.entityType,
    entityId: widget.entityId,
  );

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref
          .read(notificationsProvider.notifier)
          .markRead(widget.entityType, widget.entityId);
    });
  }

  void _onScroll() {
    final pos = _scrollController.position;
    if (pos.pixels >= pos.maxScrollExtent - 200) {
      ref.read(entityChatProvider(_ctx).notifier).loadMore();
    }
  }

  @override
  void dispose() {
    _scrollController.removeListener(_onScroll);
    _scrollController.dispose();
    super.dispose();
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          0,
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOut,
        );
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final chatState = ref.watch(entityChatProvider(_ctx));
    final notifier = ref.read(entityChatProvider(_ctx).notifier);

    ref.listen(entityChatProvider(_ctx), (prev, next) {
      if (next.messages.length != (prev?.messages.length ?? 0)) {
        _scrollToBottom();
      }
      if (next.error != null && next.error != prev?.error) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(next.error!),
            backgroundColor: const Color(0xFFFF6B6B),
          ),
        );
      }
    });

    final messageById = {for (final m in chatState.messages) m.id: m};
    final showThinking = chatState.isThinking;
    final chatItems = buildChatItems(chatState.messages);
    final isLoadingMore = chatState.isLoadingMore;
    final extraTop = isLoadingMore ? 1 : 0;

    return Scaffold(
      backgroundColor: const Color(0xFFF4F8FF),
      body: Column(
        children: [
          ConnieHeader(
            onBack: () => context.pop(),
            onMenu: () => showConnieCommandSheet(
              context,
              onGoals: () => context.push('/goals'),
              onProfile: () => context.push('/profile'),
              onCycle: () => context.push('/cycle'),
            ),

            subtitle: widget.title,
          ),
          Expanded(
            child: chatState.isFetching && chatState.messages.isEmpty
                ? const Center(
                    child: CircularProgressIndicator(color: Color(0xFF7B6EFF)),
                  )
                : ListView.separated(
                    controller: _scrollController,
                    reverse: true,
                    padding: const EdgeInsets.fromLTRB(14, 24, 14, 14),
                    itemCount: chatItems.length + (showThinking ? 1 : 0) + extraTop,
                    separatorBuilder: (_, _) => const SizedBox(height: 10),
                    itemBuilder: (_, i) {
                      if (showThinking && i == 0) return const ConnieLoadingBubble();
                      final adjustedI = showThinking ? i - 1 : i;
                      if (isLoadingMore && adjustedI == chatItems.length) {
                        return const Padding(
                          padding: EdgeInsets.symmetric(vertical: 12),
                          child: Center(child: SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Color(0xFF7B6EFF)))),
                        );
                      }
                      final itemIndex = chatItems.length - 1 - adjustedI;
                      final item = chatItems[itemIndex];
                      if (item is ChatDividerItem) {
                        return ChatDateDivider(date: item.date);
                      }
                      final message = item as Message;
                      final replyTarget = message.replyToId != null
                          ? messageById[message.replyToId]
                          : null;
                      return MessageBubble(
                        key: ValueKey(message.id),
                        message: message,
                        replyTarget: replyTarget,
                        onReply: notifier.setReply,
                      );
                    },
                  ),
          ),
          ConnieInputBar(
            enabled: !showThinking,
            replyingTo: chatState.replyingTo,
            onCancelReply: notifier.clearReply,
            onSend: (text) =>
                notifier.send(text, replyToId: chatState.replyingTo?.id),
          ),
        ],
      ),
    );
  }
}
