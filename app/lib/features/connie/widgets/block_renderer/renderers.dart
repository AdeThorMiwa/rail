import 'package:flutter/material.dart';
import 'package:flutter_markdown_plus/flutter_markdown_plus.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:rail/core/commands/command_dispatcher.dart';
import 'package:rail/features/connie/data/models/block_models.dart';
import 'package:rail/features/connie/data/models/chat_models.dart';
import 'package:rail/features/connie/providers/connie_provider.dart';
import 'package:rail/features/connie/widgets/block_renderer/base.dart';
import 'package:rail/features/connie/widgets/block_renderer/span.dart';

class TextBlockRenderer extends BlockRenderer<TextBlock> {
  const TextBlockRenderer({required super.block, super.key});

  @override
  Widget build(BuildContext context) {
    return SpanText(spans: block.content.spans);
  }
}

class TableBlockRenderer extends BlockRenderer<TableBlock> {
  const TableBlockRenderer({required super.block, super.key});

  @override
  Widget build(BuildContext context) {
    final hasColumns = block.columns != null && block.columns!.isNotEmpty;

    return Container(
      decoration: BoxDecoration(
        border: Border.all(color: const Color(0xFFE8E8F0)),
        borderRadius: BorderRadius.circular(10),
      ),
      clipBehavior: Clip.antiAlias,
      child: Table(
        border: TableBorder.symmetric(
          inside: const BorderSide(color: Color(0xFFE8E8F0)),
        ),
        defaultColumnWidth: const FlexColumnWidth(),
        children: [
          if (hasColumns)
            TableRow(
              decoration: const BoxDecoration(color: Color(0xFFF5F4FF)),
              children: block.columns!
                  .map((col) => _cell(col, bold: true))
                  .toList(),
            ),
          ...block.rows.map(
            (row) =>
                TableRow(children: row.map((cell) => _cell(cell)).toList()),
          ),
        ],
      ),
    );
  }

  Widget _cell(String text, {bool bold = false}) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      child: Text(
        text,
        style: TextStyle(
          fontSize: 13,
          fontWeight: bold ? FontWeight.w600 : FontWeight.normal,
          color: const Color(0xFF2D2D3A),
        ),
      ),
    );
  }
}

class ListBlockRenderer extends BlockRenderer<ListBlock> {
  const ListBlockRenderer({required super.block, super.key});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: block.items.asMap().entries.map((entry) {
        return Padding(
          padding: EdgeInsets.only(top: entry.key == 0 ? 0 : 4),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Padding(
                padding: const EdgeInsets.only(top: 3, right: 8),
                child: Container(
                  width: 5,
                  height: 5,
                  decoration: const BoxDecoration(
                    color: Color(0xFF9B8FFF),
                    shape: BoxShape.circle,
                  ),
                ),
              ),
              Expanded(
                child: MarkdownBody(
                  data: entry.value,
                  shrinkWrap: true,
                  styleSheet: MarkdownStyleSheet.fromTheme(Theme.of(context))
                      .copyWith(
                        p: const TextStyle(
                          fontSize: 14,
                          color: Color(0xFF2D2D3A),
                        ),
                        strong: const TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.w700,
                          color: Color(0xFF2D2D3A),
                        ),
                      ),
                ),
              ),
            ],
          ),
        );
      }).toList(),
    );
  }
}

class ActionsBlockRenderer extends ConsumerStatefulWidget {
  final ActionsBlock block;
  final Message? parentMessage;

  const ActionsBlockRenderer({
    required this.block,
    this.parentMessage,
    super.key,
  });

  @override
  ConsumerState<ActionsBlockRenderer> createState() =>
      _ActionsBlockRendererState();
}

class _ActionsBlockRendererState extends ConsumerState<ActionsBlockRenderer> {
  bool _isLoading = false;

  @override
  void didUpdateWidget(ActionsBlockRenderer old) {
    super.didUpdateWidget(old);
    // SSE message_updated arrived — new block data clears loading
    if (_isLoading && old.block != widget.block) {
      setState(() => _isLoading = false);
    }
  }

  Future<void> _onTap(ActionItem item) async {
    if (_isLoading || item.command == 'noop') return;

    if (item.isAsync) {
      setState(() => _isLoading = true);
    }

    try {
      await CommandDispatcher.dispatch(
        context,
        ref,
        item.command,
        item.params,
        parentMessage: widget.parentMessage,
      );
      if (!mounted) return;
      if (item.isAsync) {
        // Persist terminal state; message_updated SSE will clear loading
        final message = widget.parentMessage;
        if (message != null) {
          ref
              .read(chatRepositoryProvider)
              .resolveActionBlock(
                message.id,
                item.id,
                widget.block.successItems ?? [],
              )
              .catchError((_) {});
        }
      }
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(e.toString()),
          backgroundColor: const Color(0xFFFF6B6B),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final items = widget.block.items;

    // Pre-resolved states from server
    if (items.isEmpty) return const SizedBox.shrink();
    if (items.every((i) => i.command == 'noop')) {
      return Wrap(
        spacing: 8,
        runSpacing: 8,
        children: items
            .map((item) => _ActionButton(item: item, isDisabled: true))
            .toList(),
      );
    }

    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: items.map((item) {
        return _ActionButton(
          item: item,
          isLoading: _isLoading,
          isDisabled: _isLoading,
          onTap: () => _onTap(item),
        );
      }).toList(),
    );
  }
}

class _ActionButton extends StatelessWidget {
  final ActionItem item;
  final bool isLoading;
  final bool isDisabled;
  final VoidCallback? onTap;

  const _ActionButton({
    required this.item,
    this.isLoading = false,
    this.isDisabled = false,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final style = item.style;
    final isPrimary =
        style == 'primary' || style == 'resume' || style == 'success';
    final isDanger = style == 'drop';

    return FilledButton(
      onPressed: (isLoading || isDisabled) ? null : onTap,
      style: FilledButton.styleFrom(
        backgroundColor: isDanger
            ? const Color(0xFFFFEBEE)
            : isPrimary
            ? const Color(0xFF6355EE)
            : const Color(0xFFF0ECFF),
        foregroundColor: isDanger
            ? const Color(0xFFE53935)
            : isPrimary
            ? Colors.white
            : const Color(0xFF6355EE),
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
        elevation: 0,
        tapTargetSize: MaterialTapTargetSize.shrinkWrap,
      ),
      child: isLoading
          ? const SizedBox(
              width: 10,
              height: 10,
              child: CircularProgressIndicator(
                strokeWidth: 2,
                color: Color(0xFF6355EE),
              ),
            )
          : Text(
              item.label,
              style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600),
            ),
    );
  }
}

class ImageBlockRenderer extends BlockRenderer<ImageBlock> {
  const ImageBlockRenderer({required super.block, super.key});

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(10),
      child: AspectRatio(
        aspectRatio: block.aspectRatio,
        child: Image.network(block.url, fit: BoxFit.cover),
      ),
    );
  }
}

class WrapCardBlockRenderer extends BlockRenderer<WrapCardBlock> {
  const WrapCardBlockRenderer({required super.block, super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF6355EE).withOpacity(0.15),
            blurRadius: 20,
            offset: const Offset(0, 6),
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [_buildHeader(), _buildBody()],
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Container(
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          colors: [Color(0xFF6355EE), Color(0xFFA897FF)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
      ),
      padding: const EdgeInsets.fromLTRB(20, 18, 20, 18),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '✦  Cycle Wrap',
            style: GoogleFonts.nunito(
              fontSize: 11,
              fontWeight: FontWeight.w800,
              color: Colors.white.withOpacity(0.75),
              letterSpacing: 1.2,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            block.cycleTitle,
            style: GoogleFonts.nunito(
              fontSize: 20,
              fontWeight: FontWeight.w900,
              color: Colors.white,
            ),
          ),
          const SizedBox(height: 2),
          Text(
            block.period,
            style: GoogleFonts.nunito(
              fontSize: 13,
              fontWeight: FontWeight.w600,
              color: Colors.white.withOpacity(0.8),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBody() {
    return Container(
      color: Colors.white,
      padding: const EdgeInsets.fromLTRB(20, 18, 20, 20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (block.focusGoals.isNotEmpty) ...[
            _sectionLabel('Focus Goals'),
            const SizedBox(height: 10),
            ...block.focusGoals.map(_buildGoalRow),
            const SizedBox(height: 16),
          ],
          if (block.habitHighlights.isNotEmpty) ...[
            _sectionLabel('Habit Highlights'),
            const SizedBox(height: 10),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: block.habitHighlights.map(_buildHabitChip).toList(),
            ),
            const SizedBox(height: 16),
          ],
          if (block.keyWins.isNotEmpty) ...[
            _sectionLabel('Key Wins'),
            const SizedBox(height: 8),
            ...block.keyWins.map(_buildWinRow),
            const SizedBox(height: 16),
          ],
          if (block.summary.isNotEmpty)
            Text(
              block.summary,
              style: GoogleFonts.nunito(
                fontSize: 13,
                fontStyle: FontStyle.italic,
                color: const Color(0xFF9090AA),
                height: 1.5,
              ),
            ),
        ],
      ),
    );
  }

  Widget _sectionLabel(String text) {
    return Text(
      text.toUpperCase(),
      style: GoogleFonts.nunito(
        fontSize: 10,
        fontWeight: FontWeight.w900,
        color: const Color(0xFF6355EE),
        letterSpacing: 1.1,
      ),
    );
  }

  Widget _buildGoalRow(WrapCardFocusGoal goal) {
    final pct = (goal.completionRate * 100).round();
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  goal.title,
                  style: GoogleFonts.nunito(
                    fontSize: 13,
                    fontWeight: FontWeight.w700,
                    color: const Color(0xFF1A1A2E),
                  ),
                ),
              ),
              Text(
                '$pct%',
                style: GoogleFonts.nunito(
                  fontSize: 13,
                  fontWeight: FontWeight.w800,
                  color: pct >= 70
                      ? const Color(0xFF4CAF50)
                      : pct >= 40
                      ? const Color(0xFFFF9800)
                      : const Color(0xFFEF5350),
                ),
              ),
            ],
          ),
          const SizedBox(height: 5),
          ClipRRect(
            borderRadius: BorderRadius.circular(4),
            child: LinearProgressIndicator(
              value: goal.completionRate.clamp(0.0, 1.0),
              backgroundColor: const Color(0xFFF0ECFF),
              valueColor: AlwaysStoppedAnimation<Color>(
                pct >= 70
                    ? const Color(0xFF4CAF50)
                    : pct >= 40
                    ? const Color(0xFFFF9800)
                    : const Color(0xFFEF5350),
              ),
              minHeight: 6,
            ),
          ),
          Text(
            '${goal.completed} of ${goal.total} tasks done',
            style: GoogleFonts.nunito(
              fontSize: 11,
              color: const Color(0xFF9090AA),
              fontWeight: FontWeight.w600,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildHabitChip(WrapCardHabitHighlight h) {
    final pct = (h.adherenceRate * 100).round();
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(
        color: const Color(0xFFF0ECFF),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Text(
        '${h.title}  $pct%',
        style: GoogleFonts.nunito(
          fontSize: 12,
          fontWeight: FontWeight.w700,
          color: const Color(0xFF6355EE),
        ),
      ),
    );
  }

  Widget _buildWinRow(String win) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 5),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Padding(
            padding: EdgeInsets.only(top: 1),
            child: Icon(
              Icons.check_circle_rounded,
              size: 14,
              color: Color(0xFF4CAF50),
            ),
          ),
          const SizedBox(width: 6),
          Expanded(
            child: Text(
              win,
              style: GoogleFonts.nunito(
                fontSize: 13,
                fontWeight: FontWeight.w600,
                color: const Color(0xFF1A1A2E),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class RedirectBlockRenderer extends BlockRenderer<RedirectBlock> {
  const RedirectBlockRenderer({required super.block, super.key});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () => context.push(block.route),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
        decoration: BoxDecoration(
          gradient: const LinearGradient(
            colors: [Color(0xFF6355EE), Color(0xFF9B8FFF)],
          ),
          borderRadius: BorderRadius.circular(12),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              block.label,
              style: GoogleFonts.nunito(
                fontSize: 14,
                fontWeight: FontWeight.w800,
                color: Colors.white,
              ),
            ),
            const SizedBox(width: 6),
            const Icon(
              Icons.chevron_right_rounded,
              size: 16,
              color: Colors.white,
            ),
          ],
        ),
      ),
    );
  }
}

class UnsupportedBlockView extends BlockRenderer<Block> {
  const UnsupportedBlockView({required super.block, super.key});

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      spacing: 6,
      children: [
        Icon(
          Icons.info_outline,
          size: 14,
          color: Theme.of(context).colorScheme.outline,
        ),
        Text(
          "You received a message we can't render",
          style: Theme.of(context).textTheme.bodySmall?.copyWith(
            color: Theme.of(context).colorScheme.outline,
            fontStyle: FontStyle.italic,
          ),
        ),
      ],
    );
  }
}
