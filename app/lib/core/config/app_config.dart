abstract final class AppConfig {
  static const String apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://localhost:8080',
  );

  static const List<String> googleScopes = ['email', 'profile'];

  static const String googleServerClientId = String.fromEnvironment(
    'GOOGLE_SERVER_CLIENT_ID',
    defaultValue: '',
  );

  static const int authRefreshMaxAttempts = int.fromEnvironment(
    'AUTH_REFRESH_MAX_ATTEMPTS',
    defaultValue: 2,
  );
}
