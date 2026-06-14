import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../events/sse_event_bus.dart';
import 'notification_item.dart';
import 'notifications_store.dart';

final notificationsStoreProvider = Provider<NotificationsStore>((_) => NotificationsStore());

final notificationsProvider = NotifierProvider<NotificationsNotifier, List<NotificationItem>>(
  NotificationsNotifier.new,
);

class NotificationsNotifier extends Notifier<List<NotificationItem>> {
  NotificationsStore get _store => ref.read(notificationsStoreProvider);
  StreamSubscription? _sub;

  @override
  List<NotificationItem> build() {
    _loadFromStore();
    _sub = ref
        .read(sseEventBusProvider)
        .stream
        .where((e) => e.type == 'notification')
        .listen((e) => _onNotification(e.data));
    ref.onDispose(() => _sub?.cancel());
    return [];
  }

  Future<void> _loadFromStore() async {
    final items = await _store.load();
    state = items;
  }

  Future<void> _onNotification(Map<String, dynamic> data) async {
    final item = NotificationItem.fromJson(data);
    await _store.add(item);
    state = [item, ...state.where((n) => n.id != item.id)];
  }

  Future<void> markRead(String entityType, String entityId) async {
    await _store.removeByEntity(entityType, entityId);
    state = state
        .where((n) => !(n.entityType == entityType && n.entityId == entityId))
        .toList();
  }

  int get unreadCount => state.length;
}
