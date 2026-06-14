import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:rail/features/connie/data/models/chat_models.dart';
import 'message_preview.dart';

class ReplyQuote extends StatelessWidget {
  final Message replyTarget;

  const ReplyQuote({required this.replyTarget, super.key});

  @override
  Widget build(BuildContext context) {
    final senderLabel = replyTarget.isFromUser ? 'You' : 'Connie';
    final preview = messagePreviewText(replyTarget.blocks);

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: Colors.black.withValues(alpha: 0.06),
        borderRadius: BorderRadius.circular(8),
        border: const Border(
          left: BorderSide(color: Color(0xFF6355EE), width: 3),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            senderLabel,
            style: GoogleFonts.nunito(
              fontSize: 11,
              fontWeight: FontWeight.w800,
              color: Colors.white,
            ),
          ),
          const SizedBox(height: 2),
          Text(
            preview,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
            style: GoogleFonts.nunito(
              fontSize: 12,
              fontWeight: FontWeight.w600,
              color: Colors.white,
            ),
          ),
        ],
      ),
    );
  }
}
