import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:rail/features/connie/widgets/connie_command_sheet.dart';
import '../../../../core/notifications/notifications_provider.dart';
import '../../intentions/data/models/goal_models.dart';
import '../../intentions/providers/goals_provider.dart';
import '../data/models/block_models.dart';
import '../data/models/chat_models.dart';
import '../providers/goal_chat_provider.dart';
import '../widgets/connie_header.dart';
import '../widgets/connie_input_bar.dart';
import '../widgets/connie_loading_bubble.dart';
import '../widgets/message_bubble.dart';

class GoalChatScreen extends ConsumerStatefulWidget {
  final String goalPid;

  const GoalChatScreen({super.key, required this.goalPid});

  @override
  ConsumerState<GoalChatScreen> createState() => _GoalChatScreenState();
}

class _GoalChatScreenState extends ConsumerState<GoalChatScreen> {
  final _scrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(notificationsProvider.notifier).markRead('GOAL', widget.goalPid);
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

  Map<String, String> _buildTaskTitleMap(GoalDetail goal) {
    final map = <String, String>{};
    for (final m in goal.milestones) {
      for (final t in m.tasks) map[t.pid] = t.title;
    }
    for (final t in goal.orphanTasks) map[t.pid] = t.title;
    return map;
  }

  @override
  Widget build(BuildContext context) {
    final chatState = ref.watch(goalChatProvider(widget.goalPid));
    final notifier = ref.read(goalChatProvider(widget.goalPid).notifier);
    final goalAsync = ref.watch(goalDetailProvider(widget.goalPid));
    final goalTitle = goalAsync.valueOrNull?.title ?? '';
    final taskTitles = goalAsync.valueOrNull != null
        ? _buildTaskTitleMap(goalAsync.valueOrNull!)
        : <String, String>{};

    ref.listen(goalChatProvider(widget.goalPid), (prev, next) {
      if (next.activity.length != (prev?.activity.length ?? 0)) {
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

    final items = chatState.activity;
    final messageById = {for (final m in items) m.id: m};
    final showThinking = chatState.isThinking;

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
            subtitle: goalTitle.isNotEmpty ? goalTitle : '…',
          ),
          Expanded(
            child: chatState.isFetching && items.isEmpty
                ? const Center(
                    child: CircularProgressIndicator(color: Color(0xFF7B6EFF)),
                  )
                : items.isEmpty && !showThinking
                ? _EmptyState(goalTitle: goalTitle)
                : ListView.separated(
                    controller: _scrollController,
                    reverse: true,
                    padding: const EdgeInsets.fromLTRB(14, 24, 14, 14),
                    itemCount: items.length + (showThinking ? 1 : 0),
                    separatorBuilder: (_, _) => const SizedBox(height: 8),
                    itemBuilder: (_, i) {
                      if (showThinking && i == 0) {
                        return const ConnieLoadingBubble();
                      }
                      final idx = items.length - 1 - (showThinking ? i - 1 : i);
                      final msg = items[idx];
                      final replyTarget = msg.replyToId != null
                          ? messageById[msg.replyToId]
                          : null;

                      if (msg.chatEntityType == 'TASK') {
                        return _TaskActivityCard(
                          message: msg,
                          taskTitle: taskTitles[msg.chatEntityId] ?? 'Task',
                        );
                      }

                      return MessageBubble(
                        key: ValueKey(msg.id),
                        message: msg,
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

class _TaskActivityCard extends StatelessWidget {
  final Message message;
  final String taskTitle;

  const _TaskActivityCard({required this.message, required this.taskTitle});

  String _extractText() {
    for (final block in message.blocks) {
      if (block is TextBlock) {
        final text = block.content.spans
            .whereType<ConnieTextSpan>()
            .map((s) => s.text)
            .where((t) => t.isNotEmpty)
            .join(' ');
        if (text.isNotEmpty) return text;
      }
    }
    return '';
  }

  @override
  Widget build(BuildContext context) {
    final isUser = message.isFromUser;
    final text = _extractText();

    return Align(
      alignment: isUser ? Alignment.centerRight : Alignment.centerLeft,
      child: ConstrainedBox(
        constraints: BoxConstraints(
          maxWidth: MediaQuery.of(context).size.width * 0.78,
        ),
        child: Container(
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(10),
            border: Border.all(color: const Color(0xFFEEEEF5), width: 1.5),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                padding: const EdgeInsets.fromLTRB(12, 8, 12, 6),
                decoration: const BoxDecoration(
                  color: Color(0xFFF8F6FF),
                  borderRadius: BorderRadius.vertical(top: Radius.circular(9)),
                ),
                child: Row(
                  children: [
                    Container(
                      width: 6,
                      height: 6,
                      decoration: const BoxDecoration(
                        color: Color(0xFF9B8FFF),
                        shape: BoxShape.circle,
                      ),
                    ),
                    const SizedBox(width: 6),
                    Expanded(
                      child: Text(
                        taskTitle,
                        overflow: TextOverflow.ellipsis,
                        style: GoogleFonts.nunito(
                          fontSize: 11,
                          fontWeight: FontWeight.w800,
                          color: const Color(0xFF7B6EFF),
                        ),
                      ),
                    ),
                    Text(
                      isUser ? 'You' : 'Connie',
                      style: GoogleFonts.nunito(
                        fontSize: 10,
                        fontWeight: FontWeight.w700,
                        color: const Color(0xFFAAAAC0),
                      ),
                    ),
                  ],
                ),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(12, 8, 12, 10),
                child: Text(
                  text,
                  style: GoogleFonts.nunito(
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color: const Color(0xFF3A3A5C),
                    height: 1.5,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _EmptyState extends StatelessWidget {
  final String goalTitle;

  const _EmptyState({required this.goalTitle});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text('💬', style: TextStyle(fontSize: 36)),
            const SizedBox(height: 16),
            Text(
              'No activity yet',
              style: GoogleFonts.nunito(
                fontSize: 16,
                fontWeight: FontWeight.w800,
                color: const Color(0xFF3A3A5C),
              ),
            ),
            const SizedBox(height: 8),
            Text(
              'This is where notes, reflections, and Connie\'s messages about "$goalTitle" will appear.',
              textAlign: TextAlign.center,
              style: GoogleFonts.nunito(
                fontSize: 13,
                fontWeight: FontWeight.w600,
                color: const Color(0xFF9090AA),
                height: 1.6,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
