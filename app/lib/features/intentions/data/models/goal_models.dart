enum GoalType { habit, abstinence, project, task, quantified }

class HabitStats {
  final int currentStreak;
  final int bestStreak;
  final int thisWeekDone;
  final int thisWeekTarget;
  final int missedThisWeek;
  final double thisWeekRate;
  final double allTimeRate;
  final List<String> weekDotStatuses;
  final List<String> monthDotStatuses;
  final int slipsTotal;

  const HabitStats({
    required this.currentStreak,
    required this.bestStreak,
    required this.thisWeekDone,
    required this.thisWeekTarget,
    required this.missedThisWeek,
    required this.thisWeekRate,
    required this.allTimeRate,
    required this.weekDotStatuses,
    required this.monthDotStatuses,
    this.slipsTotal = 0,
  });

  factory HabitStats.fromJson(Map<String, dynamic> j) => HabitStats(
        currentStreak: (j['currentStreak'] as num?)?.toInt() ?? 0,
        bestStreak: (j['bestStreak'] as num?)?.toInt() ?? 0,
        thisWeekDone: (j['thisWeekDone'] as num?)?.toInt() ?? 0,
        thisWeekTarget: (j['thisWeekTarget'] as num?)?.toInt() ?? 7,
        missedThisWeek: (j['missedThisWeek'] as num?)?.toInt() ?? 0,
        thisWeekRate: (j['thisWeekRate'] as num?)?.toDouble() ?? 0.0,
        allTimeRate: (j['allTimeRate'] as num?)?.toDouble() ?? 0.0,
        weekDotStatuses: List<String>.from(j['weekDotStatuses'] as List? ?? []),
        monthDotStatuses:
            List<String>.from(j['monthDotStatuses'] as List? ?? []),
        slipsTotal: (j['slipsTotal'] as num?)?.toInt() ?? 0,
      );

  static HabitStats empty() => const HabitStats(
        currentStreak: 0,
        bestStreak: 0,
        thisWeekDone: 0,
        thisWeekTarget: 7,
        missedThisWeek: 0,
        thisWeekRate: 0,
        allTimeRate: 0,
        weekDotStatuses: [],
        monthDotStatuses: [],
        slipsTotal: 0,
      );
}

enum GoalStatus { active, blocked, paused, completed, abandoned }

enum MilestoneStatus { pending, inProgress, done }

GoalType _parseGoalType(String s) => switch (s) {
      'HABIT' => GoalType.habit,
      'ABSTINENCE' => GoalType.abstinence,
      'PROJECT' => GoalType.project,
      'QUANTIFIED' => GoalType.quantified,
      _ => GoalType.task,
    };

GoalStatus _parseGoalStatus(String s) => switch (s) {
      'BLOCKED' => GoalStatus.blocked,
      'PAUSED' => GoalStatus.paused,
      'COMPLETED' => GoalStatus.completed,
      'ABANDONED' => GoalStatus.abandoned,
      _ => GoalStatus.active,
    };

MilestoneStatus _parseMilestoneStatus(String s) => switch (s) {
      'DONE' => MilestoneStatus.done,
      'IN_PROGRESS' => MilestoneStatus.inProgress,
      _ => MilestoneStatus.pending,
    };

// ── List model ────────────────────────────────────────────────────────────────

class GoalListItem {
  final String pid;
  final String title;
  final String intentionTitle;
  final GoalType type;
  final GoalStatus status;
  final int milestonesDone;
  final int milestonesTotal;
  final int pendingTaskCount;
  final int estimatedTotalHours;
  final int actualTotalHours;
  final GoalTargetData? target;
  final HabitStats? habitStats;
  final String? currentMilestoneTitle;
  final String? firstPendingTaskTitle;

  const GoalListItem({
    required this.pid,
    required this.title,
    required this.intentionTitle,
    required this.type,
    required this.status,
    required this.milestonesDone,
    required this.milestonesTotal,
    required this.pendingTaskCount,
    required this.estimatedTotalHours,
    required this.actualTotalHours,
    this.target,
    this.habitStats,
    this.currentMilestoneTitle,
    this.firstPendingTaskTitle,
  });

  double get milestoneProgress =>
      milestonesTotal > 0 ? milestonesDone / milestonesTotal : 0;

  factory GoalListItem.fromJson(Map<String, dynamic> j) {
    final milestones = (j['milestones'] as List<dynamic>?) ?? [];
    final tasks = (j['todaysTasks'] as List<dynamic>?) ?? [];
    final done = milestones.where((m) => (m as Map)['status'] == 'DONE').length;
    final pending = tasks.where((t) => (t as Map)['status'] != 'DONE').length;

    String? currentMilestone;
    for (final m in milestones) {
      if ((m as Map)['status'] != 'DONE') {
        currentMilestone = m['title'] as String?;
        break;
      }
    }

    String? firstTask;
    for (final t in tasks) {
      if ((t as Map)['status'] != 'DONE') {
        firstTask = t['title'] as String?;
        break;
      }
    }

    final targetJson = j['target'] as Map<String, dynamic>?;
    final habitStatsJson = j['habitStats'] as Map<String, dynamic>?;

    return GoalListItem(
      pid: j['pid'] as String,
      title: j['title'] as String,
      intentionTitle: j['intentionTitle'] as String? ?? '',
      type: _parseGoalType(j['type'] as String),
      status: _parseGoalStatus(j['status'] as String),
      milestonesDone: done,
      milestonesTotal: milestones.length,
      pendingTaskCount: pending,
      estimatedTotalHours: (j['estimatedTotalHours'] as num?)?.toInt() ?? 0,
      actualTotalHours: (j['actualTotalHours'] as num?)?.toInt() ?? 0,
      target: targetJson != null ? GoalTargetData.fromJson(targetJson) : null,
      habitStats: habitStatsJson != null ? HabitStats.fromJson(habitStatsJson) : null,
      currentMilestoneTitle: currentMilestone,
      firstPendingTaskTitle: firstTask,
    );
  }
}

// ── Detail models ─────────────────────────────────────────────────────────────

class TaskDetail {
  final String pid;
  final String title;
  final String? notes;
  final bool isDone;
  final int? durationMinutes;
  final String? milestonePid;

  const TaskDetail({
    required this.pid,
    required this.title,
    this.notes,
    required this.isDone,
    this.durationMinutes,
    this.milestonePid,
  });

  factory TaskDetail.fromJson(Map<String, dynamic> j) => TaskDetail(
        pid: j['pid'] as String,
        title: j['title'] as String,
        notes: j['notes'] as String?,
        isDone: (j['status'] as String?) == 'DONE',
        durationMinutes: j['durationMinutes'] as int?,
        milestonePid: j['milestonePid'] as String?,
      );
}

class MilestoneDetail {
  final String pid;
  final String title;
  final MilestoneStatus status;
  final List<TaskDetail> tasks;

  const MilestoneDetail({
    required this.pid,
    required this.title,
    required this.status,
    required this.tasks,
  });

  bool get isDone => status == MilestoneStatus.done;
  bool get isActive => status == MilestoneStatus.inProgress;
  int get pendingTaskCount => tasks.where((t) => !t.isDone).length;
}

class GoalTargetData {
  final double targetValue;
  final double currentValue;
  final String unit;

  const GoalTargetData({
    required this.targetValue,
    required this.currentValue,
    required this.unit,
  });

  double get progress =>
      targetValue > 0 ? (currentValue / targetValue).clamp(0.0, 1.0) : 0.0;

  factory GoalTargetData.fromJson(Map<String, dynamic> j) => GoalTargetData(
        targetValue: (j['targetValue'] as num?)?.toDouble() ?? 0,
        currentValue: (j['currentValue'] as num?)?.toDouble() ?? 0,
        unit: j['unit'] as String? ?? '',
      );
}

class GoalDetail {
  final String pid;
  final String title;
  final GoalType type;
  final String intentionTitle;
  final int estimatedTotalHours;
  final int actualTotalHours;
  final GoalTargetData? target;
  final HabitStats? habitStats;
  final List<MilestoneDetail> milestones;
  final List<TaskDetail> orphanTasks;

  const GoalDetail({
    required this.pid,
    required this.title,
    required this.type,
    required this.intentionTitle,
    required this.estimatedTotalHours,
    required this.actualTotalHours,
    this.target,
    this.habitStats,
    required this.milestones,
    required this.orphanTasks,
  });

  double get milestoneProgress => milestones.isEmpty
      ? 0
      : milestones.where((m) => m.isDone).length / milestones.length;

  factory GoalDetail.fromJson(Map<String, dynamic> j) {
    final allTasks = (j['todaysTasks'] as List<dynamic>? ?? [])
        .map((t) => TaskDetail.fromJson(t as Map<String, dynamic>))
        .toList();

    final milestones = ((j['milestones'] as List<dynamic>?) ?? []).map((m) {
      final mj = m as Map<String, dynamic>;
      final mPid = mj['pid'] as String;
      return MilestoneDetail(
        pid: mPid,
        title: mj['title'] as String,
        status: _parseMilestoneStatus(mj['status'] as String),
        tasks: allTasks.where((t) => t.milestonePid == mPid).toList(),
      );
    }).toList();

    final targetJson = j['target'] as Map<String, dynamic>?;
    final habitStatsJson = j['habitStats'] as Map<String, dynamic>?;

    return GoalDetail(
      pid: j['pid'] as String,
      title: j['title'] as String,
      type: _parseGoalType(j['type'] as String),
      intentionTitle: j['intentionTitle'] as String? ?? '',
      estimatedTotalHours: (j['estimatedTotalHours'] as num?)?.toInt() ?? 0,
      actualTotalHours: (j['actualTotalHours'] as num?)?.toInt() ?? 0,
      target: targetJson != null ? GoalTargetData.fromJson(targetJson) : null,
      habitStats: habitStatsJson != null ? HabitStats.fromJson(habitStatsJson) : null,
      milestones: milestones,
      orphanTasks: allTasks.where((t) => t.milestonePid == null).toList(),
    );
  }
}
