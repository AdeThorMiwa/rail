import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../features/auth/providers/auth_provider.dart';
import '../../features/auth/screens/auth_screen.dart';
import '../../features/connie/screens/connie_screen.dart';
import '../../features/connie/screens/entity_chat_screen.dart';
import '../../features/connie/screens/goal_chat_screen.dart';
import '../../features/home/screens/home_screen.dart';
import '../../features/home/screens/profile_screen.dart';
import '../../features/intentions/screens/goal_detail_screen.dart';
import '../../features/intentions/screens/goals_screen.dart';
import '../../features/notifications/screens/notifications_screen.dart';
import '../../features/onboarding/providers/intro_seen_provider.dart';
import '../../features/onboarding/screens/onboarding_screen.dart';
import '../../features/cycles/screens/cycle_screen.dart';
import '../../features/scheduling/screens/scheduling_profile_screen.dart';

final goRouterProvider = Provider<GoRouter>((ref) {
  final notifier = _RouterNotifier(ref);

  return GoRouter(
    refreshListenable: notifier,
    initialLocation: '/home',
    redirect: (context, state) {
      final authState = ref.read(authProvider);
      final introSeen = ref.read(introSeenProvider);

      if (authState.isLoading || introSeen.isLoading) return null;

      final loc = state.matchedLocation;
      final isAuthenticated = authState.valueOrNull is AuthAuthenticated;

      if (!isAuthenticated) {
        if (introSeen.valueOrNull != true) {
          return loc == '/intro' ? null : '/intro';
        }
        return loc == '/auth' ? null : '/auth';
      }

      // Authenticated from here
      final auth = authState.valueOrNull as AuthAuthenticated;
      if (!auth.onboardingComplete) {
        return loc == '/schedule-setup' ? null : '/schedule-setup';
      }
      if (loc == '/intro' || loc == '/auth' || loc == '/schedule-setup') {
        return '/home';
      }
      return null;
    },
    routes: [
      GoRoute(path: '/intro', builder: (_, _) => const OnboardingScreen()),
      GoRoute(path: '/auth', builder: (_, _) => const AuthScreen()),
      GoRoute(path: '/schedule-setup', builder: (_, _) => const SchedulingProfileScreen()),
      GoRoute(path: '/home', builder: (_, _) => const HomeScreen()),
      GoRoute(path: '/connie', builder: (_, _) => const ConnieScreen()),
      GoRoute(path: '/goals', builder: (_, _) => const GoalsScreen()),
      GoRoute(path: '/profile', builder: (_, _) => const ProfileScreen()),
      GoRoute(
        path: '/goals/:pid',
        builder: (_, state) =>
            GoalDetailScreen(goalPid: state.pathParameters['pid']!),
      ),
      GoRoute(
        path: '/goals/:pid/chat',
        builder: (_, state) =>
            GoalChatScreen(goalPid: state.pathParameters['pid']!),
      ),
      GoRoute(path: '/cycle', builder: (_, _) => const CycleScreen()),
      GoRoute(path: '/notifications', builder: (_, _) => const NotificationsScreen()),
      GoRoute(
        path: '/chat/:entityType/:entityId',
        builder: (_, state) => EntityChatScreen(
          entityType: state.pathParameters['entityType']!,
          entityId: state.pathParameters['entityId']!,
          title: state.uri.queryParameters['title'] ?? '',
        ),
      ),
    ],
  );
});

class _RouterNotifier extends ChangeNotifier {
  _RouterNotifier(Ref ref) {
    ref.listen(authProvider, (_, _) => notifyListeners());
    ref.listen(introSeenProvider, (_, _) => notifyListeners());
  }
}
