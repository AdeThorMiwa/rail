import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';
import 'notification_item.dart';

const _kKey = 'rail_notifications';
const _kTtlDays = 7;

class NotificationsStore {
  Future<List<NotificationItem>> load() async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getStringList(_kKey) ?? [];
    final cutoff = DateTime.now().subtract(const Duration(days: _kTtlDays));
    return raw
        .map((s) => NotificationItem.fromJson(jsonDecode(s) as Map<String, dynamic>))
        .where((n) => n.timestamp.isAfter(cutoff))
        .toList();
  }

  Future<void> save(List<NotificationItem> items) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setStringList(
      _kKey,
      items.map((n) => jsonEncode(n.toJson())).toList(),
    );
  }

  Future<void> add(NotificationItem item) async {
    final current = await load();
    final updated = [item, ...current.where((n) => n.id != item.id)];
    await save(updated);
  }

  Future<void> removeByEntity(String entityType, String entityId) async {
    final current = await load();
    final updated = current
        .where((n) => !(n.entityType == entityType && n.entityId == entityId))
        .toList();
    await save(updated);
  }
}
