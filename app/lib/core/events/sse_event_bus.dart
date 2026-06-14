import 'dart:async';
import 'dart:math';

import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../features/auth/providers/auth_provider.dart';
import 'events_repository.dart';
import 'sse_event.dart';

final sseEventBusProvider = Provider<SseEventBus>((ref) {
  final bus = SseEventBus(ref.read(eventsRepositoryProvider));
  ref.onDispose(bus.dispose);
  return bus;
});

// Watches auth state and starts/stops the SSE connection accordingly.
// Must be watched somewhere that stays alive while the user is logged in.
final sseConnectionManagerProvider = Provider<void>((ref) {
  final bus = ref.watch(sseEventBusProvider);
  ref.listen<AsyncValue<AuthState>>(authProvider, (_, next) {
    if (next.valueOrNull is AuthAuthenticated) {
      bus.start();
    } else {
      bus.stop();
    }
  }, fireImmediately: true);
});

class SseEventBus {
  final EventsRepository _repository;
  final _controller = StreamController<SseEvent>.broadcast();
  StreamSubscription<SseEvent>? _sub;
  int _retryCount = 0;
  bool _disposed = false;
  bool _running = false;

  Stream<SseEvent> get stream => _controller.stream;

  SseEventBus(this._repository);

  void start() {
    if (_disposed || _running) return;
    _running = true;
    _connect();
  }

  void stop() {
    _running = false;
    _retryCount = 0;
    _sub?.cancel();
    _sub = null;
    if (kDebugMode) debugPrint('[SseEventBus] stopped');
  }

  void _connect() {
    if (_disposed || !_running) return;
    _sub?.cancel();
    _sub = _repository.subscribe().listen(
      (event) {
        _retryCount = 0;
        if (!_disposed) _controller.add(event);
      },
      onError: (e) {
        if (kDebugMode) debugPrint('[SseEventBus] error: $e — reconnecting');
        _scheduleReconnect();
      },
      onDone: () {
        if (kDebugMode) debugPrint('[SseEventBus] done — reconnecting');
        _scheduleReconnect();
      },
    );
  }

  void _scheduleReconnect() {
    if (_disposed || !_running) return;
    final seconds = min(30, pow(2, _retryCount).toInt());
    _retryCount++;
    if (kDebugMode) debugPrint('[SseEventBus] reconnecting in ${seconds}s (attempt $_retryCount)');
    Future.delayed(Duration(seconds: seconds), _connect);
  }

  void dispose() {
    _disposed = true;
    _sub?.cancel();
    _controller.close();
  }
}
