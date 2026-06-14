import 'package:flutter/material.dart';
import 'package:flutter_markdown_plus/flutter_markdown_plus.dart';
import 'package:rail/features/connie/data/models/block_models.dart';
import 'package:rail/features/connie/data/models/chat_models.dart';
import 'block_renderer/block_factory.dart';
import 'reply_quote.dart';
import 'swipe_to_reply.dart';

class MessageBubble extends StatelessWidget {
  final Message message;
  final Message? replyTarget;
  final ValueChanged<Message>? onReply;

  const MessageBubble({
    required this.message,
    this.replyTarget,
    this.onReply,
    super.key,
  });

  @override
  Widget build(BuildContext context) {
    final bubble = message.isFromUser
        ? _UserBubble(message: message, replyTarget: replyTarget)
        : _ConnieBubble(message: message, replyTarget: replyTarget);

    if (onReply == null) return bubble;

    return SwipeToReply(
      onReply: () => onReply!(message),
      child: bubble,
    );
  }
}

class _UserBubble extends StatelessWidget {
  final Message message;
  final Message? replyTarget;

  const _UserBubble({required this.message, this.replyTarget});

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: Alignment.centerRight,
      child: Container(
        constraints: BoxConstraints(
          maxWidth: MediaQuery.of(context).size.width * 0.75,
        ),
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            colors: [Color(0xFF6355EE), Color(0xFF6355EE)],
          ),
          borderRadius: BorderRadius.only(
            topLeft: Radius.circular(16),
            topRight: Radius.circular(16),
            bottomLeft: Radius.circular(16),
            bottomRight: Radius.circular(4),
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (replyTarget != null) ...[
              ReplyQuote(replyTarget: replyTarget!),
              const SizedBox(height: 8),
            ],
            MarkdownBody(
              data: _getMessageText(),
              styleSheet:
                  MarkdownStyleSheet.fromTheme(Theme.of(context)).copyWith(
                p: Theme.of(context)
                    .textTheme
                    .bodyMedium
                    ?.copyWith(color: Colors.white),
                strong: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  fontWeight: FontWeight.w700,
                  color: Colors.white,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  String _getMessageText() {
    return switch (message.blocks.first) {
      TextBlock t => switch (t.content.spans.first) {
        ConnieTextSpan s => s.text,
        _ => '',
      },
      _ => '',
    };
  }
}

class _ConnieBubble extends StatelessWidget {
  final Message message;
  final Message? replyTarget;

  const _ConnieBubble({required this.message, this.replyTarget});

  Color get _borderColor => switch (message.variant) {
    'warning' => const Color(0xFFFFB74D),
    'confirm' => const Color(0xFF66BB6A),
    _ => Colors.transparent,
  };

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.end,
      children: [
        Container(
          width: 30,
          height: 30,
          decoration: const BoxDecoration(
            shape: BoxShape.circle,
            gradient: LinearGradient(
              colors: [Color(0xFF6355EE), Color(0xFF9B8FFF)],
            ),
          ),
          child: const Center(
            child: Text('🎩', style: TextStyle(fontSize: 13)),
          ),
        ),
        const SizedBox(width: 10),
        Flexible(
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
            decoration: BoxDecoration(
              color: Colors.white,
              border: Border.all(color: _borderColor, width: 1.5),
              borderRadius: const BorderRadius.only(
                topLeft: Radius.circular(16),
                topRight: Radius.circular(16),
                bottomRight: Radius.circular(16),
                bottomLeft: Radius.circular(4),
              ),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withValues(alpha: 0.05),
                  blurRadius: 6,
                  offset: const Offset(0, 2),
                ),
              ],
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              spacing: 8,
              children: [
                if (replyTarget != null) ReplyQuote(replyTarget: replyTarget!),
                ...message.blocks.map(
                  (b) => BlockRendererFactory.build(b, parentMessage: message),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}
