class NotificationItem {
  final String id;
  final String entityType;
  final String entityId;
  final String label;
  final String preview;
  final DateTime timestamp;

  const NotificationItem({
    required this.id,
    required this.entityType,
    required this.entityId,
    required this.label,
    required this.preview,
    required this.timestamp,
  });

  factory NotificationItem.fromJson(Map<String, dynamic> json) {
    return NotificationItem(
      id: json['id'] as String,
      entityType: json['entityType'] as String,
      entityId: json['entityId'] as String? ?? '',
      label: json['label'] as String,
      preview: json['preview'] as String,
      timestamp: DateTime.parse(json['timestamp'] as String),
    );
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'entityType': entityType,
    'entityId': entityId,
    'label': label,
    'preview': preview,
    'timestamp': timestamp.toIso8601String(),
  };
}
