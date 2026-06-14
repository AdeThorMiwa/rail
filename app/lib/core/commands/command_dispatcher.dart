import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:rail/features/connie/data/models/chat_models.dart';
import 'package:rail/features/connie/providers/connie_provider.dart';
import 'package:rail/features/intentions/providers/goals_provider.dart';

class CommandDispatcher {
  const CommandDispatcher._();

  static Future<void> dispatch(
    BuildContext context,
    WidgetRef ref,
    String command,
    Map<String, dynamic> params, {
    Message? parentMessage,
  }) async {
    switch (command.toLowerCase()) {
      case 'navigation.navigate':
        final route = params['path'] as String?;
        if (route != null) context.push(route);

      case 'intentions.confirm':
        final proposalId = params['proposalId'] as String?;
        if (proposalId != null) {
          await ref.read(goalsProvider.notifier).confirmProposal(proposalId);
        }

      case 'chats.reply':
        if (parentMessage != null) {
          ref.read(connieProvider.notifier).setReply(parentMessage);
        }

      case 'noop':
        break;

      default:
        if (kDebugMode) {
          debugPrint('[CommandDispatcher] Unknown command: $command');
        }
    }
  }
}
