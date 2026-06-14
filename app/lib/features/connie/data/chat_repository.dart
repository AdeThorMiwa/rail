import 'package:rail/features/connie/data/models/block_models.dart';
import 'package:rail/features/connie/data/models/chat_models.dart';

import '../../../core/network/api_client.dart';

class ChatRepository {
  final ApiClient _client;

  const ChatRepository(this._client);

  Future<Chat> get() async {
    final res = await _client.dio.get('/chat');
    return Chat.fromJson(res.data as Map<String, dynamic>);
  }

  Future<List<Message>> getMessages() async {
    final res = await _client.dio.get('/chat/messages');
    return (res.data as List)
        .map((e) => Message.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<void> send(String rawInput, {String? replyToId}) async {
    await _client.dio.post('/chat/messages', data: {
      'rawInput': rawInput,
      'replyToId': replyToId,
    });
  }

  Future<void> resolveActionBlock(
    String messagePid,
    String tappedItemId,
    List<ActionItem> resolvedItems,
  ) async {
    await _client.dio.patch('/chat/messages/$messagePid/action-block', data: {
      'tappedItemId': tappedItemId,
      'resolvedItems': resolvedItems.map((i) => i.toJson()).toList(),
    });
  }

  Future<List<Message>> getEntityMessages(String entityType, String entityId) async {
    final res = await _client.dio.get('/chat/$entityType/$entityId/messages');
    return (res.data as List)
        .map((e) => Message.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<List<Message>> getGoalActivity(String goalPid) async {
    final res = await _client.dio.get('/goals/$goalPid/activity');
    return (res.data as List)
        .map((e) => Message.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<void> sendEntityMessage(
    String entityType,
    String entityId,
    String rawInput, {
    String? replyToId,
  }) async {
    await _client.dio.post('/chat/$entityType/$entityId/messages', data: {
      'rawInput': rawInput,
      if (replyToId != null) 'replyToId': replyToId,
    });
  }

}
