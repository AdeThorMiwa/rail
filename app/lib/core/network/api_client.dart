import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'token_manager.dart';

const _refreshTokenKey = 'refresh_token';

class ApiClient {
  late final Dio dio;
  final TokenManager tokenManager;
  final FlutterSecureStorage storage;
  final String baseUrl;
  final void Function() onSessionExpired;

  ApiClient({
    required this.baseUrl,
    required this.tokenManager,
    required this.storage,
    required this.onSessionExpired,
  }) {
    dio = Dio(
      BaseOptions(
        baseUrl: baseUrl,
        connectTimeout: const Duration(seconds: 20),
        receiveTimeout: const Duration(seconds: 20),
        headers: {'Content-Type': 'application/json'},
      ),
    );
    dio.interceptors.add(_AuthInterceptor(this));
  }
}

class _AuthInterceptor extends Interceptor {
  final ApiClient _client;
  bool _isRefreshing = false;

  _AuthInterceptor(this._client);

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    final token = _client.tokenManager.accessToken;
    if (token != null) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
  }

  @override
  Future<void> onError(
    DioException err,
    ErrorInterceptorHandler handler,
  ) async {
    final status = err.response?.statusCode;
    if ((status == 401 || status == 403) && !_isRefreshing) {
      _isRefreshing = true;
      try {
        final refreshToken = await _client.storage.read(key: _refreshTokenKey);
        if (refreshToken == null) {
          _client.onSessionExpired();
          return handler.reject(err);
        }

        final refreshDio = Dio(BaseOptions(baseUrl: _client.baseUrl));
        final res = await refreshDio.post(
          '/auth/refresh',
          data: {'refreshToken': refreshToken},
        );

        final newAccessToken = res.data['accessToken'] as String;
        final newRefreshToken = res.data['refreshToken'] as String;

        _client.tokenManager.setToken(newAccessToken);
        await _client.storage.write(
          key: _refreshTokenKey,
          value: newRefreshToken,
        );

        final retryOptions = err.requestOptions;
        retryOptions.headers['Authorization'] = 'Bearer $newAccessToken';
        final retryResponse = await _client.dio.fetch(retryOptions);
        return handler.resolve(retryResponse);
      } catch (_) {
        await _client.storage.delete(key: _refreshTokenKey);
        _client.tokenManager.clear();
        _client.onSessionExpired();
        return handler.reject(err);
      } finally {
        _isRefreshing = false;
      }
    }
    handler.next(err);
  }
}
