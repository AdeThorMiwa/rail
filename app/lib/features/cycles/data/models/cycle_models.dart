enum CycleStatus { planned, active, inReview, completed }

class CycleFocusGoal {
  final String goalPid;
  final String title;
  final String type;
  final int position;

  const CycleFocusGoal({
    required this.goalPid,
    required this.title,
    required this.type,
    required this.position,
  });

  factory CycleFocusGoal.fromJson(Map<String, dynamic> j) => CycleFocusGoal(
        goalPid: j['goalPid'] as String,
        title: j['title'] as String,
        type: (j['type'] as String).toLowerCase(),
        position: j['position'] as int,
      );
}

class UserCycle {
  final String pid;
  final String title;
  final DateTime startDate;
  final DateTime endDate;
  final String reviewTime;
  final CycleStatus status;

  const UserCycle({
    required this.pid,
    required this.title,
    required this.startDate,
    required this.endDate,
    required this.reviewTime,
    required this.status,
  });

  bool get isActive => status == CycleStatus.active;
  bool get isPlanned => status == CycleStatus.planned;
  bool get isInReview => status == CycleStatus.inReview;

  int get daysRemaining {
    final end = DateTime(endDate.year, endDate.month, endDate.day);
    final today = DateTime.now();
    final todayFlat = DateTime(today.year, today.month, today.day);
    final diff = end.difference(todayFlat).inDays;
    return diff < 0 ? 0 : diff;
  }

  factory UserCycle.fromJson(Map<String, dynamic> j) => UserCycle(
        pid: j['pid'] as String,
        title: j['title'] as String,
        startDate: DateTime.parse(j['startDate'] as String),
        endDate: DateTime.parse(j['endDate'] as String),
        reviewTime: j['reviewTime'] as String,
        status: _parseStatus(j['status'] as String),
      );

  static CycleStatus _parseStatus(String s) => switch (s) {
        'ACTIVE' => CycleStatus.active,
        'IN_REVIEW' => CycleStatus.inReview,
        'COMPLETED' => CycleStatus.completed,
        _ => CycleStatus.planned,
      };
}
