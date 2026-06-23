import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:rail/features/connie/data/models/chat_models.dart';

class ChatDividerItem {
  final DateTime date;
  const ChatDividerItem(this.date);
}

/// Returns a chronologically-ordered list where a [ChatDividerItem] is inserted
/// before the first [Message] of each calendar day.
List<Object> buildChatItems(List<Message> messages) {
  final items = <Object>[];
  DateTime? lastDay;
  for (final msg in messages) {
    final day = DateTime(msg.createdAt.year, msg.createdAt.month, msg.createdAt.day);
    if (lastDay == null || day != lastDay) {
      items.add(ChatDividerItem(msg.createdAt));
      lastDay = day;
    }
    items.add(msg);
  }
  return items;
}

class ChatDateDivider extends StatelessWidget {
  final DateTime date;
  const ChatDateDivider({required this.date, super.key});

  String get _label {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final day = DateTime(date.year, date.month, date.day);
    final diff = today.difference(day).inDays;
    if (diff == 0) return 'Today';
    if (diff == 1) return 'Yesterday';
    const months = [
      'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
      'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec',
    ];
    final monthStr = months[date.month - 1];
    return date.year == now.year
        ? '$monthStr ${date.day}'
        : '$monthStr ${date.day}, ${date.year}';
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        children: [
          const Expanded(child: Divider(color: Color(0xFFD8D8EC), thickness: 1)),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12),
            child: Text(
              _label,
              style: GoogleFonts.nunito(
                fontSize: 11,
                fontWeight: FontWeight.w700,
                color: const Color(0xFFAAAAC0),
                letterSpacing: 0.3,
              ),
            ),
          ),
          const Expanded(child: Divider(color: Color(0xFFD8D8EC), thickness: 1)),
        ],
      ),
    );
  }
}
