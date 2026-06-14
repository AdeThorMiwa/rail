class TokenManager {
  String? _accessToken;

  String? get accessToken => _accessToken;

  void setToken(String? token) => _accessToken = token;

  void clear() => _accessToken = null;
}
