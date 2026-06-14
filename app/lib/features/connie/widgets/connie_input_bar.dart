import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:rail/features/connie/data/models/chat_models.dart';
import 'message_preview.dart';

class ConnieInputBar extends StatefulWidget {
  final bool enabled;
  final ValueChanged<String> onSend;
  final Message? replyingTo;
  final VoidCallback? onCancelReply;

  const ConnieInputBar({
    super.key,
    required this.onSend,
    this.enabled = true,
    this.replyingTo,
    this.onCancelReply,
  });

  @override
  State<ConnieInputBar> createState() => _ConnieInputBarState();
}

class _ConnieInputBarState extends State<ConnieInputBar> {
  final _controller = TextEditingController();
  final _focusNode = FocusNode();

  @override
  void didUpdateWidget(ConnieInputBar old) {
    super.didUpdateWidget(old);
    if (widget.replyingTo != null && old.replyingTo == null) {
      _focusNode.requestFocus();
    }
  }

  void _submit() {
    final text = _controller.text.trim();
    if (text.isEmpty || !widget.enabled) return;
    _controller.clear();
    widget.onSend(text);
  }

  @override
  void dispose() {
    _controller.dispose();
    _focusNode.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: Colors.white,
        border: Border(top: BorderSide(color: Color(0xFFF0ECFF), width: 1.5)),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (widget.replyingTo != null)
            _ReplyStrip(
              message: widget.replyingTo!,
              onCancel: widget.onCancelReply ?? () {},
            ),
          Padding(
            padding: EdgeInsets.fromLTRB(
              16, 10, 16, MediaQuery.of(context).padding.bottom + 10,
            ),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Expanded(
                  child: TextField(
                    controller: _controller,
                    focusNode: _focusNode,
                    enabled: widget.enabled,
                    minLines: 1,
                    maxLines: 5,
                    keyboardType: TextInputType.multiline,
                    textInputAction: TextInputAction.newline,
                    style: GoogleFonts.nunito(
                      fontSize: 14,
                      fontWeight: FontWeight.w600,
                      color: const Color(0xFF1A1A2E),
                    ),
                    decoration: InputDecoration(
                      hintText: 'Message Connie…',
                      hintStyle: GoogleFonts.nunito(
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                        color: const Color(0xFFAAAAC0),
                      ),
                      filled: true,
                      fillColor: const Color(0xFFF4F8FF),
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(20),
                        borderSide: BorderSide.none,
                      ),
                      contentPadding: const EdgeInsets.symmetric(
                        horizontal: 16,
                        vertical: 10,
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                GestureDetector(
                  onTap: _submit,
                  child: Container(
                    width: 38,
                    height: 38,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      gradient: const LinearGradient(
                        colors: [Color(0xFF7B6EFF), Color(0xFF9B8FFF)],
                      ),
                      boxShadow: [
                        BoxShadow(
                          color: const Color(0xFF7B6EFF).withValues(alpha: 0.32),
                          blurRadius: 10,
                          offset: const Offset(0, 3),
                        ),
                      ],
                    ),
                    child: const Center(
                      child: Icon(
                        Icons.send_rounded,
                        size: 17,
                        color: Colors.white,
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _ReplyStrip extends StatelessWidget {
  final Message message;
  final VoidCallback onCancel;

  const _ReplyStrip({required this.message, required this.onCancel});

  @override
  Widget build(BuildContext context) {
    final isUser = message.isFromUser;
    final senderLabel = isUser ? 'You' : 'Connie';
    final preview = messagePreviewText(message.blocks);

    return Container(
      padding: const EdgeInsets.fromLTRB(12, 8, 8, 8),
      color: const Color(0xFFF0ECFF),
      child: Row(
        children: [
          Container(
            width: 3,
            height: 36,
            decoration: BoxDecoration(
              color: const Color(0xFF6355EE),
              borderRadius: BorderRadius.circular(2),
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  senderLabel,
                  style: GoogleFonts.nunito(
                    fontSize: 12,
                    fontWeight: FontWeight.w800,
                    color: const Color(0xFF6355EE),
                  ),
                ),
                Text(
                  preview,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: GoogleFonts.nunito(
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                    color: const Color(0xFF5A5A7A),
                  ),
                ),
              ],
            ),
          ),
          GestureDetector(
            onTap: onCancel,
            child: const Padding(
              padding: EdgeInsets.all(6),
              child: Icon(Icons.close, size: 16, color: Color(0xFF9090AA)),
            ),
          ),
        ],
      ),
    );
  }
}

