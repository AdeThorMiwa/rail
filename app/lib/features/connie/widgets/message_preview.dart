import 'package:rail/features/connie/data/models/block_models.dart';

String messagePreviewText(List<Block> blocks) {
  for (final block in blocks) {
    if (block is TextBlock) {
      final text = block.content.spans
          .whereType<ConnieTextSpan>()
          .map((s) => s.text)
          .join();
      if (text.isNotEmpty) return text;
    }
  }
  return '📎 Message';
}
