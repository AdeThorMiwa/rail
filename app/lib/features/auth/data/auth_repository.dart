import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../../../core/config/app_config.dart';
import '../../../core/network/api_client.dart';
import 'models/auth_models.dart';

const _refreshTokenKey = 'refresh_token';

class AuthRepository {
  final ApiClient _client;
  final FlutterSecureStorage _storage;

  const AuthRepository(this._client, this._storage);

  Future<AuthResponse> register(String email, String password, String displayName) async {
    final res = await _client.dio.post('/auth/register', data: {
      'email': email,
      'password': password,
      'displayName': displayName,
    });
    return _handleAuthResponse(res.data as Map<String, dynamic>);
  }

  Future<AuthResponse> login(String email, String password) async {
    final res = await _client.dio.post('/auth/login', data: {
      'email': email,
      'password': password,
    });
    return _handleAuthResponse(res.data as Map<String, dynamic>);
  }

  Future<AuthResponse> googleAuth(String idToken) async {
    final res = await _client.dio.post('/auth/google', data: {'idToken': idToken});
    return _handleAuthResponse(res.data as Map<String, dynamic>);
  }

  Future<AuthResponse?> tryRefresh() async {
    final refreshToken = await _storage.read(key: _refreshTokenKey);
    if (refreshToken == null) return null;

    for (var attempt = 0; attempt < AppConfig.authRefreshMaxAttempts; attempt++) {
      try {
        final res = await _client.dio.post('/auth/refresh', data: {'refreshToken': refreshToken});
        return _handleAuthResponse(res.data as Map<String, dynamic>);
      } on DioException catch (e) {
        final status = e.response?.statusCode;
        if (status == 401 || status == 403) {
          await _storage.delete(key: _refreshTokenKey);
          return null;
        }
        if (attempt == AppConfig.authRefreshMaxAttempts - 1) return null;
      } on Exception {
        if (attempt == AppConfig.authRefreshMaxAttempts - 1) return null;
      }
    }
    return null;
  }

  Future<void> logout(String refreshToken) async {
    try {
      await _client.dio.post('/auth/logout', data: {'refreshToken': refreshToken});
    } catch (_) {}
    await _storage.delete(key: _refreshTokenKey);
  }

  Future<String?> getStoredRefreshToken() => _storage.read(key: _refreshTokenKey);

  Future<AuthResponse> _handleAuthResponse(Map<String, dynamic> data) async {
    final auth = AuthResponse.fromJson(data);
    await _storage.write(key: _refreshTokenKey, value: auth.refreshToken);
    return auth;
  }
}
