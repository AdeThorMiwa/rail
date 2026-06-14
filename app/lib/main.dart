import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import 'core/events/sse_event_bus.dart';
import 'core/notifications/alarm_scheduler.dart';
import 'core/notifications/notifications_provider.dart';
import 'core/router/app_router.dart';
import 'features/auth/providers/auth_provider.dart';
import 'features/connie/providers/connie_provider.dart';
import 'features/connie/providers/entity_chat_provider.dart';
import 'features/connie/providers/goal_chat_provider.dart';
import 'features/cycles/providers/cycle_provider.dart';
import 'features/home/providers/home_provider.dart';
import 'features/intentions/providers/goals_provider.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await AlarmScheduler.init();
  runApp(const ProviderScope(child: RailApp()));
}

class RailApp extends ConsumerWidget {
  const RailApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final authState = ref.watch(authProvider);
    ref.watch(sseConnectionManagerProvider);

    ref.listen(authProvider, (prev, next) {
      final wasAuthenticated = prev?.valueOrNull is AuthAuthenticated;
      final isNowUnauthenticated = next.valueOrNull is AuthUnauthenticated;
      if (wasAuthenticated && isNowUnauthenticated) {
        ref.invalidate(connieProvider);
        ref.invalidate(entityChatProvider);
        ref.invalidate(goalChatProvider);
        ref.invalidate(goalsProvider);
        ref.invalidate(goalDetailProvider);
        ref.invalidate(cycleProvider);
        ref.invalidate(cycleFocusesProvider);
        ref.invalidate(todayScheduleProvider);
        ref.invalidate(schedulingProfileProvider);
        ref.invalidate(notificationsProvider);
      }
    });

    if (authState.isLoading) {
      return const MaterialApp(
        debugShowCheckedModeBanner: false,
        home: Scaffold(
          backgroundColor: Color(0xFFF4F8FF),
          body: Center(
            child: CircularProgressIndicator(color: Color(0xFF7B6EFF)),
          ),
        ),
      );
    }

    final router = ref.watch(goRouterProvider);

    return MaterialApp.router(
      title: 'Rail',
      debugShowCheckedModeBanner: false,
      theme: _buildTheme(),
      routerConfig: router,
    );
  }

  ThemeData _buildTheme() {
    final base = ThemeData(
      brightness: Brightness.light,
      scaffoldBackgroundColor: const Color(0xFFF4F8FF),
      colorScheme: const ColorScheme.light(
        primary: Color(0xFF7B6EFF),
        surface: Colors.white,
      ),
      useMaterial3: true,
    );
    return base.copyWith(
      textTheme: GoogleFonts.nunitoTextTheme(base.textTheme),
    );
  }
}
