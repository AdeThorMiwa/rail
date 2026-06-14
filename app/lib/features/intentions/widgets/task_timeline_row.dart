import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../data/models/goal_models.dart';

class TaskTimelineRow extends StatelessWidget {
  final TaskDetail task;

  const TaskTimelineRow({super.key, required this.task});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.only(top: 3),
            child: Container(
              width: 10,
              height: 10,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: task.isDone
                    ? const Color(0xFF7B6EFF)
                    : Colors.white,
                border: Border.all(
                  color: task.isDone
                      ? const Color(0xFF7B6EFF)
                      : const Color(0xFFD8D4F0),
                  width: 1.5,
                ),
              ),
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  task.title,
                  style: GoogleFonts.nunito(
                    fontSize: 13,
                    fontWeight: FontWeight.w700,
                    color: task.isDone
                        ? const Color(0xFF9090AA)
                        : const Color(0xFF1A1A2E),
                    decoration:
                        task.isDone ? TextDecoration.lineThrough : null,
                  ),
                ),
                if (task.durationMinutes != null) ...[
                  const SizedBox(height: 2),
                  Text(
                    '${task.durationMinutes} min',
                    style: GoogleFonts.nunito(
                      fontSize: 11,
                      fontWeight: FontWeight.w700,
                      color: const Color(0xFFAAAAC0),
                    ),
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}
