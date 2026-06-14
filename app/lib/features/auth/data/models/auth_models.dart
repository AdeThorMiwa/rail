class UserInfo {
  final String pid;
  final String email;
  final String displayName;

  const UserInfo({
    required this.pid,
    required this.email,
    required this.displayName,
  });

  factory UserInfo.fromJson(Map<String, dynamic> json) => UserInfo(
        pid: json['pid'] as String,
        email: json['email'] as String,
        displayName: json['displayName'] as String,
      );
}

class AuthResponse {
  final String accessToken;
  final String refreshToken;
  final UserInfo user;
  final bool onboardingComplete;

  const AuthResponse({
    required this.accessToken,
    required this.refreshToken,
    required this.user,
    required this.onboardingComplete,
  });

  factory AuthResponse.fromJson(Map<String, dynamic> json) => AuthResponse(
        accessToken: json['accessToken'] as String,
        refreshToken: json['refreshToken'] as String,
        user: UserInfo.fromJson(json['user'] as Map<String, dynamic>),
        onboardingComplete: json['onboardingComplete'] as bool,
      );
}
