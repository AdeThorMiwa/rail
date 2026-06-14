import 'package:rail/features/connie/data/models/block_models.dart';

enum MessageSender { connie, user }

class Message {
  final String id;
  final String conversationId;
  final MessageSender sender;
  final List<Block> blocks;
  final String variant;
  final DateTime createdAt;
  final String? replyToId;
  final String chatEntityType;
  final String? chatEntityId;

  const Message({
    required this.id,
    required this.conversationId,
    required this.sender,
    required this.blocks,
    this.variant = 'default',
    required this.createdAt,
    this.replyToId,
    this.chatEntityType = 'GLOBAL',
    this.chatEntityId,
  });

  factory Message.fromJson(Map<String, dynamic> json) {
    return Message(
      id: json['id'] as String,
      conversationId: json['conversationId'] as String,
      sender: _parseSender(json['senderId'] as String),
      blocks: (json['blocks'] as List<dynamic>)
          .map((e) => Block.fromJson(e as Map<String, dynamic>))
          .toList(),
      variant: json['variant'] as String? ?? 'default',
      createdAt: DateTime.parse(json['createdAt'] as String),
      replyToId: json['replyToId'] as String?,
      chatEntityType: json['chatEntityType'] as String? ?? 'GLOBAL',
      chatEntityId: json['chatEntityId'] as String?,
    );
  }

  static MessageSender _parseSender(String type) {
    return switch (type) {
      'user' => MessageSender.user,
      _ => MessageSender.connie,
    };
  }

  bool get isFromUser => sender == MessageSender.user;
}

class ConnieTurn {
  final Message userMessage;
  final Message connieMessage;

  const ConnieTurn({required this.userMessage, required this.connieMessage});

  factory ConnieTurn.fromJson(Map<String, dynamic> json) => ConnieTurn(
    userMessage: Message.fromJson(json['userMessage'] as Map<String, dynamic>),
    connieMessage: Message.fromJson(
      json['connieMessage'] as Map<String, dynamic>,
    ),
  );
}

class Chat {
  final String id;

  const Chat({required this.id});

  factory Chat.fromJson(Map<String, dynamic> json) {
    return Chat(id: json['id'] as String);
  }
}

