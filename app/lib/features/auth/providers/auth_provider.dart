import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../../../core/config/app_config.dart';
import '../../../core/network/api_client.dart';
import '../../../core/network/token_manager.dart';
import '../data/auth_repository.dart';
import '../data/models/auth_models.dart';

final tokenManagerProvider = Provider<TokenManager>((ref) => TokenManager());

final secureStorageProvider = Provider<FlutterSecureStorage>(
  (ref) => const FlutterSecureStorage(),
);

final apiClientProvider = Provider<ApiClient>((ref) {
  return ApiClient(
    baseUrl: AppConfig.apiBaseUrl,
    tokenManager: ref.read(tokenManagerProvider),
    storage: ref.read(secureStorageProvider),
    onSessionExpired: () => ref.read(authProvider.notifier).clearSession(),
  );
});

final authRepositoryProvider = Provider<AuthRepository>((ref) {
  return AuthRepository(
    ref.watch(apiClientProvider),
    ref.watch(secureStorageProvider),
  );
});

sealed class AuthState {
  const AuthState();
}

class AuthUnauthenticated extends AuthState {
  const AuthUnauthenticated();
}

class AuthAuthenticated extends AuthState {
  final UserInfo user;
  final bool onboardingComplete;
  const AuthAuthenticated(this.user, {required this.onboardingComplete});
}

class AuthNotifier extends AsyncNotifier<AuthState> {
  @override
  Future<AuthState> build() async {
    final auth = await ref.read(authRepositoryProvider).tryRefresh();
    if (auth != null) {
      ref.read(tokenManagerProvider).setToken(auth.accessToken);
      return AuthAuthenticated(auth.user, onboardingComplete: auth.onboardingComplete);
    }
    return const AuthUnauthenticated();
  }

  Future<void> register(String email, String password, String displayName) async {
    final auth = await ref
        .read(authRepositoryProvider)
        .register(email, password, displayName);
    ref.read(tokenManagerProvider).setToken(auth.accessToken);
    state = AsyncData(AuthAuthenticated(auth.user, onboardingComplete: auth.onboardingComplete));
  }

  Future<void> login(String email, String password) async {
    final auth = await ref.read(authRepositoryProvider).login(email, password);
    ref.read(tokenManagerProvider).setToken(auth.accessToken);
    state = AsyncData(AuthAuthenticated(auth.user, onboardingComplete: auth.onboardingComplete));
  }

  Future<void> googleSignIn(String idToken) async {
    final auth = await ref.read(authRepositoryProvider).googleAuth(idToken);
    ref.read(tokenManagerProvider).setToken(auth.accessToken);
    state = AsyncData(AuthAuthenticated(auth.user, onboardingComplete: auth.onboardingComplete));
  }

  Future<void> logout() async {
    final refreshToken = await ref
        .read(secureStorageProvider)
        .read(key: 'refresh_token');
    if (refreshToken != null) {
      await ref.read(authRepositoryProvider).logout(refreshToken);
    }
    ref.read(tokenManagerProvider).clear();
    state = const AsyncData(AuthUnauthenticated());
  }

  void onboardingCompleted() {
    final current = state.valueOrNull;
    if (current is AuthAuthenticated) {
      state = AsyncData(AuthAuthenticated(current.user, onboardingComplete: true));
    }
  }

  void clearSession() {
    ref.read(tokenManagerProvider).clear();
    state = const AsyncData(AuthUnauthenticated());
  }
}

final authProvider = AsyncNotifierProvider<AuthNotifier, AuthState>(
  AuthNotifier.new,
);

