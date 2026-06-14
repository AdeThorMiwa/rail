enum IntentionType { bounded, unbounded }

enum IntentionStatus { active, paused, completed, abandoned }

enum GoalType { habit, abstinence, project, task, quantified }

enum MilestoneStatus { pending, inProgress, done }

GoalType _parseGoalType(String s) => switch (s) {
  'HABIT' => GoalType.habit,
  'ABSTINENCE' => GoalType.abstinence,
  'PROJECT' => GoalType.project,
  'QUANTIFIED' => GoalType.quantified,
  _ => GoalType.task,
};

IntentionStatus _parseIntentionStatus(String s) => switch (s) {
  'PAUSED' => IntentionStatus.paused,
  'COMPLETED' => IntentionStatus.completed,
  'ABANDONED' => IntentionStatus.abandoned,
  _ => IntentionStatus.active,
};

MilestoneStatus _parseMilestoneStatus(String s) => switch (s) {
  'DONE' => MilestoneStatus.done,
  'IN_PROGRESS' => MilestoneStatus.inProgress,
  _ => MilestoneStatus.pending,
};

// ── List models ──────────────────────────────────────────────────────────────

class GoalSummary {
  final String pid;
  final String title;
  final GoalType type;
  final int milestonesDone;
  final int milestonesTotal;
  final int pendingTaskCount;
  final int estimatedTotalHours;
  final int actualTotalHours;
  final String? currentMilestoneTitle;
  final String? firstPendingTaskTitle;

  const GoalSummary({
    required this.pid,
    required this.title,
    required this.type,
    required this.milestonesDone,
    required this.milestonesTotal,
    required this.pendingTaskCount,
    required this.estimatedTotalHours,
    required this.actualTotalHours,
    this.currentMilestoneTitle,
    this.firstPendingTaskTitle,
  });

  double get milestoneProgress =>
      milestonesTotal > 0 ? milestonesDone / milestonesTotal : 0;

  factory GoalSummary.fromJson(Map<String, dynamic> j) {
    final milestones = (j['milestones'] as List<dynamic>?) ?? [];
    final tasks = (j['tasks'] as List<dynamic>?) ?? [];
    final done = milestones.where((m) => (m as Map)['status'] == 'DONE').length;

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

    final pending = tasks.where((t) => (t as Map)['status'] != 'DONE').length;

    return GoalSummary(
      pid: j['pid'] as String,
      title: j['title'] as String,
      type: _parseGoalType(j['type'] as String),
      milestonesDone: done,
      milestonesTotal: milestones.length,
      pendingTaskCount: pending,
      estimatedTotalHours: (j['estimatedTotalHours'] as num?)?.toInt() ?? 0,
      actualTotalHours: (j['actualTotalHours'] as num?)?.toInt() ?? 0,
      currentMilestoneTitle: currentMilestone,
      firstPendingTaskTitle: firstTask,
    );
  }
}

class IntentionSummary {
  final String pid;
  final String rawInput;
  final String title;
  final IntentionType type;
  final IntentionStatus status;
  final DateTime createdAt;
  final GoalSummary? goal;

  const IntentionSummary({
    required this.pid,
    required this.rawInput,
    required this.title,
    required this.type,
    required this.status,
    required this.createdAt,
    this.goal,
  });

  GoalType? get goalType => goal?.type;

  factory IntentionSummary.fromJson(Map<String, dynamic> j) {
    final goalJson = j['activeGoal'] as Map<String, dynamic>?;
    return IntentionSummary(
      pid: j['pid'] as String,
      rawInput: j['rawInput'] as String,
      title: j['title'] as String,
      type: j['type'] == 'UNBOUNDED'
          ? IntentionType.unbounded
          : IntentionType.bounded,
      status: _parseIntentionStatus(j['status'] as String),
      createdAt: DateTime.parse(j['createdAt'] as String),
      goal: goalJson != null ? GoalSummary.fromJson(goalJson) : null,
    );
  }
}

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

class GoalDetail {
  final String pid;
  final String title;
  final GoalType type;
  final int estimatedTotalHours;
  final int actualTotalHours;
  final List<MilestoneDetail> milestones;
  final List<TaskDetail> orphanTasks;

  const GoalDetail({
    required this.pid,
    required this.title,
    required this.type,
    required this.estimatedTotalHours,
    required this.actualTotalHours,
    required this.milestones,
    required this.orphanTasks,
  });

  double get milestoneProgress => milestones.isEmpty
      ? 0
      : milestones.where((m) => m.isDone).length / milestones.length;

  factory GoalDetail.fromJson(Map<String, dynamic> j) {
    final allTasks = (j['tasks'] as List<dynamic>? ?? [])
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

    return GoalDetail(
      pid: j['pid'] as String,
      title: j['title'] as String,
      type: _parseGoalType(j['type'] as String),
      estimatedTotalHours: (j['estimatedTotalHours'] as num?)?.toInt() ?? 0,
      actualTotalHours: (j['actualTotalHours'] as num?)?.toInt() ?? 0,
      milestones: milestones,
      orphanTasks: allTasks.where((t) => t.milestonePid == null).toList(),
    );
  }
}

class IntentionDetail {
  final String pid;
  final String title;
  final String? completionCriteria;
  final IntentionType type;
  final IntentionStatus status;
  final DateTime createdAt;
  final GoalDetail? goal;

  const IntentionDetail({
    required this.pid,
    required this.title,
    this.completionCriteria,
    required this.type,
    required this.status,
    required this.createdAt,
    this.goal,
  });

  factory IntentionDetail.fromJson(Map<String, dynamic> j) {
    final goalJson = j['activeGoal'] as Map<String, dynamic>?;
    return IntentionDetail(
      pid: j['pid'] as String,
      title: j['title'] as String,
      completionCriteria: j['completionCriteria'] as String?,
      type: j['type'] == 'UNBOUNDED'
          ? IntentionType.unbounded
          : IntentionType.bounded,
      status: _parseIntentionStatus(j['status'] as String),
      createdAt: DateTime.parse(j['createdAt'] as String),
      goal: goalJson != null ? GoalDetail.fromJson(goalJson) : null,
    );
  }
}
