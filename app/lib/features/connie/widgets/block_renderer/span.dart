import 'package:flutter/material.dart';
import 'package:flutter_markdown_plus/flutter_markdown_plus.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:rail/core/commands/command_dispatcher.dart';
import 'package:rail/features/connie/data/models/block_models.dart';

class SpanText extends StatelessWidget {
  final List<ConnieSpan> spans;
  final Color? textColor;
  const SpanText({required this.spans, this.textColor, super.key});

  @override
  Widget build(BuildContext context) {
    return Wrap(
      crossAxisAlignment: WrapCrossAlignment.start,
      spacing: 4,
      runSpacing: 0,
      children: spans.map((span) => _buildSpan(context, span)).toList(),
    );
  }

  Widget _buildSpan(BuildContext context, ConnieSpan span) => switch (span) {
    ConnieTextSpan s => MarkdownBody(
      data: s.text,
      selectable: true,
      shrinkWrap: true,
      styleSheet: MarkdownStyleSheet.fromTheme(Theme.of(context)).copyWith(
        p: Theme.of(context).textTheme.bodyMedium?.copyWith(color: textColor),
        strong: Theme.of(context).textTheme.bodyMedium?.copyWith(
          fontWeight: FontWeight.w700,
          color: textColor,
        ),
      ),
    ),
    CtaSpan s => CtaChip(span: s),
  };
}

class CtaChip extends ConsumerWidget {
  final CtaSpan span;
  const CtaChip({required this.span, super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return GestureDetector(
      onTap: () =>
          CommandDispatcher.dispatch(context, ref, span.command, span.params),
      child: Text(
        span.label,
        style: const TextStyle(
          color: Color(0xFF6355EE),
          decoration: TextDecoration.underline,
          decorationColor: Color(0xFF6355EE),
        ),
      ),
    );
  }
}
