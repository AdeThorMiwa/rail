import 'package:flutter/material.dart';
import 'package:rail/features/connie/data/models/block_models.dart';
import 'package:rail/features/connie/data/models/chat_models.dart';
import './renderers.dart';

class BlockRendererFactory {
  const BlockRendererFactory._();

  static Widget build(Block block, {Message? parentMessage}) => switch (block) {
    TextBlock b => TextBlockRenderer(block: b),
    TableBlock b => TableBlockRenderer(block: b),
    ListBlock b => ListBlockRenderer(block: b),
    ActionsBlock b => ActionsBlockRenderer(block: b, parentMessage: parentMessage),
    ImageBlock b => ImageBlockRenderer(block: b),
    WrapCardBlock b => WrapCardBlockRenderer(block: b),
    RedirectBlock b => RedirectBlockRenderer(block: b),
  };
}
