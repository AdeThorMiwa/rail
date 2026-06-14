import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:rail/features/connie/widgets/message_bubble.dart';
import '../../../core/notifications/notifications_provider.dart';
import '../providers/connie_provider.dart';
import '../widgets/connie_command_sheet.dart';
import '../widgets/connie_header.dart';
import '../widgets/connie_input_bar.dart';
import '../widgets/connie_loading_bubble.dart';

class ConnieScreen extends ConsumerStatefulWidget {
  const ConnieScreen({super.key});

  @override
  ConsumerState<ConnieScreen> createState() => _ConnieScreenState();
}

class _ConnieScreenState extends ConsumerState<ConnieScreen> {
  final _scrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final s = ref.read(connieProvider);
      if ((s.messages.isEmpty || s.error != null) && !s.isLoading) {
        ref.read(connieProvider.notifier).refresh();
      }
      ref.read(notificationsProvider.notifier).markRead('GLOBAL', '');
    });
  }

  @override
  void dispose() {
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
    final state = ref.watch(connieProvider);
    final notifier = ref.read(connieProvider.notifier);

    ref.listen(connieProvider, (prev, next) {
      _scrollToBottom();
      if (next.error != null && next.error != prev?.error) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(next.error!),
            backgroundColor: const Color(0xFFFF6B6B),
          ),
        );
      }
    });

    final messageById = {for (final m in state.messages) m.id: m};

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
          ),
          Expanded(
            child: ListView.separated(
              controller: _scrollController,
              reverse: true,
              padding: const EdgeInsets.fromLTRB(14, 24, 14, 14),
              itemCount: state.messages.length + (state.isLoading ? 1 : 0),
              separatorBuilder: (_, _) => const SizedBox(height: 10),
              itemBuilder: (_, i) {
                if (state.isLoading && i == 0) {
                  return const ConnieLoadingBubble();
                }
                final msgIndex = state.messages.length - 1 - (state.isLoading ? i - 1 : i);
                final message = state.messages[msgIndex];
                final replyTarget = message.replyToId != null
                    ? messageById[message.replyToId]
                    : null;
                return MessageBubble(
                  key: ValueKey(message.id),
                  message: message,
                  replyTarget: replyTarget,
                  onReply: (m) => notifier.setReply(m),
                );
              },
            ),
          ),
          ConnieInputBar(
            enabled: !state.isLoading,
            replyingTo: state.replyingTo,
            onCancelReply: notifier.clearReply,
            onSend: (text) => notifier.send(text),
          ),
        ],
      ),
    );
  }
}
