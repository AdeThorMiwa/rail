import 'dart:async';
import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../network/api_client.dart';
import '../../features/auth/providers/auth_provider.dart';
import 'sse_event.dart';

final eventsRepositoryProvider = Provider<EventsRepository>((ref) {
  return EventsRepository(ref.watch(apiClientProvider));
});

class EventsRepository {
  final ApiClient _client;

  const EventsRepository(this._client);

  Stream<SseEvent> subscribe() async* {
    if (kDebugMode) debugPrint('[EventsRepository] connecting to /events');
    final response = await _client.dio.get<ResponseBody>(
      '/events',
      options: Options(
        responseType: ResponseType.stream,
        headers: {
          'Accept': 'text/event-stream',
          'Cache-Control': 'no-cache',
        },
        receiveTimeout: const Duration(days: 365),
        sendTimeout: const Duration(seconds: 10),
      ),
    );
    if (kDebugMode) debugPrint('[EventsRepository] connected, status=${response.statusCode}');

    var buffer = '';
    var eventType = '';

    await for (final chunk in response.data!.stream) {
      final raw = utf8.decode(chunk);
      buffer += raw;
      while (buffer.contains('\n\n')) {
        final idx = buffer.indexOf('\n\n');
        final block = buffer.substring(0, idx);
        buffer = buffer.substring(idx + 2);

        String? data;
        for (final line in block.split('\n')) {
          if (line.startsWith('event:')) {
            eventType = line.substring(6).trim();
          } else if (line.startsWith('data:')) {
            data = line.substring(5).trim();
          }
        }

        if (data != null && eventType.isNotEmpty) {
          if (kDebugMode) debugPrint('[EventsRepository] event=$eventType');
          yield SseEvent(
            type: eventType,
            data: jsonDecode(data) as Map<String, dynamic>,
          );
          eventType = '';
        }
      }
    }
    if (kDebugMode) debugPrint('[EventsRepository] stream ended');
  }
}
