import 'package:flutter/material.dart';
import 'package:rail/features/connie/data/models/block_models.dart';

abstract class BlockRenderer<T extends Block> extends StatelessWidget {
  final T block;
  const BlockRenderer({required this.block, super.key});
}
