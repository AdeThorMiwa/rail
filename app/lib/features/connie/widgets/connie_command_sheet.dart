import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

Future<void> showConnieCommandSheet(
  BuildContext context, {
  required VoidCallback onGoals,
  required VoidCallback onProfile,
  required VoidCallback onCycle,
}) {
  return showModalBottomSheet(
    context: context,
    backgroundColor: Colors.transparent,
    builder: (_) => _ConnieCommandSheet(
      onGoals: onGoals,
      onProfile: onProfile,
      onCycle: onCycle,
    ),
  );
}

class _ConnieCommandSheet extends StatelessWidget {
  final VoidCallback onGoals;
  final VoidCallback onProfile;
  final VoidCallback onCycle;

  const _ConnieCommandSheet({
    required this.onGoals,
    required this.onProfile,
    required this.onCycle,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.vertical(top: Radius.circular(22)),
      ),
      padding: EdgeInsets.fromLTRB(
        16,
        0,
        16,
        MediaQuery.of(context).padding.bottom + 16,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const SizedBox(height: 10),
          Container(
            width: 40,
            height: 4,
            decoration: BoxDecoration(
              color: const Color(0xFFE0DCFF),
              borderRadius: BorderRadius.circular(2),
            ),
          ),
          const SizedBox(height: 16),
          _Item(
            icon: Icons.flag_outlined,
            iconBg: const Color(0xFFF0ECFF),
            iconColor: const Color(0xFF7B6EFF),
            label: 'My Goals',
            onTap: () {
              Navigator.of(context).pop();
              onGoals();
            },
          ),
          _Item(
            icon: Icons.track_changes_rounded,
            iconBg: const Color(0xFFEDE9FF),
            iconColor: const Color(0xFF5C4FD6),
            label: 'My Cycle',
            onTap: () {
              Navigator.of(context).pop();
              onCycle();
            },
          ),
          _Item(
            icon: Icons.person_outline,
            iconBg: const Color(0xFFE8F8F7),
            iconColor: const Color(0xFF26A69A),
            label: 'My Profile',
            onTap: () {
              Navigator.of(context).pop();
              onProfile();
            },
          ),
          // _Item(
          //   icon: Icons.bar_chart_rounded,
          //   iconBg: const Color(0xFFFFF0F6),
          //   iconColor: const Color(0xFFEC407A),
          //   label: 'Weekly Progress',
          //   onTap: () => Navigator.of(context).pop(),
          // ),
          // _Item(
          //   icon: Icons.settings_outlined,
          //   iconBg: const Color(0xFFF5F5FF),
          //   iconColor: const Color(0xFF9090AA),
          //   label: 'Settings',
          //   onTap: () => Navigator.of(context).pop(),
          // ),
        ],
      ),
    );
  }
}

class _Item extends StatelessWidget {
  final IconData icon;
  final Color iconBg;
  final Color iconColor;
  final String label;
  final VoidCallback onTap;

  const _Item({
    required this.icon,
    required this.iconBg,
    required this.iconColor,
    required this.label,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      behavior: HitTestBehavior.opaque,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 10),
        child: Row(
          children: [
            Container(
              width: 40,
              height: 40,
              decoration: BoxDecoration(
                color: iconBg,
                borderRadius: BorderRadius.circular(12),
              ),
              child: Icon(icon, size: 20, color: iconColor),
            ),
            const SizedBox(width: 14),
            Text(
              label,
              style: GoogleFonts.nunito(
                fontSize: 15,
                fontWeight: FontWeight.w800,
                color: const Color(0xFF1A1A2E),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
