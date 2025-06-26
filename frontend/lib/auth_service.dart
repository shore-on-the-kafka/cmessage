import 'package:flutter/foundation.dart'; // kDebugMode 사용
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;

class AuthService {
  final String _baseUrl = "http://localhost:8080";
  final String _apiPrefix = "/api/v1/auth";
  final _storage = const FlutterSecureStorage();

  Future<String?> getToken() async {
    return await _storage.read(key: 'auth_token');
  }

  Future<void> _storeToken(String token) async {
    await _storage.write(key: 'auth_token', value: token);
  }

  Future<void> deleteToken() async {
    await _storage.delete(key: 'auth_token');
  }

  // Line 로그인 시작을 위한 URL 가져오기
  // 백엔드의 /api/v1/auth/login/line 엔드포인트는 302 리디렉션을 반환합니다.
  // 이 리디렉션 URL을 직접 받아와야 합니다.
  Future<String?> getLineLoginInitiateUrl() async {
    final client = http.Client();
    try {
      // 중요: 백엔드 주소와 API prefix를 정확히 결합해야 합니다.
      final requestUrl = Uri.parse('$_baseUrl$_apiPrefix/login/line');
      if (kDebugMode) {
        print('Requesting Line login initiate URL: $requestUrl');
      }

      final request = http.Request('GET', requestUrl);
      request.followRedirects = false; // 리디렉션을 자동으로 따르지 않도록 설정
      final streamedResponse = await client.send(request);

      if (streamedResponse.statusCode == 302 || streamedResponse.statusCode == 301 || streamedResponse.statusCode == 303 || streamedResponse.statusCode == 307 || streamedResponse.statusCode == 308) {
        final location = streamedResponse.headers['location'];
        if (kDebugMode) {
          print('Received Line login redirect URL: $location');
        }
        return location;
      } else {
        final response = await http.Response.fromStream(streamedResponse);
        if (kDebugMode) {
          print('Failed to get Line login initiate URL: ${response.statusCode}');
          print('Response body: ${response.body}');
        }
        return null;
      }
    } catch (e) {
      if (kDebugMode) {
        print('Error getting Line login initiate URL: $e');
      }
      return null;
    } finally {
      client.close();
    }
  }

  // 백엔드의 OAuth2 콜백 URL (토큰을 포함한 JSON을 반환하는 URL)
  // 이 URL은 InAppWebView에서 감지하여 토큰을 추출하는 데 사용됩니다.
  String get backendLineCallbackBaseUrl => '$_baseUrl$_apiPrefix/login/oauth2/line';


  // 이메일/비밀번호 로그인 (필요한 경우 기존 코드 유지 또는 추가)
  Future<String?> login(String email, String password) async {
    // ... (이전 답변의 일반 로그인 코드 참고)
    // 이 예제에서는 Line 로그인에 집중하므로 비워둡니다.
    if (kDebugMode) {
      print('Email/Password login not implemented in this example.');
    }
    return null;
  }

  Future<void> logout() async {
    await deleteToken();
  }

  // 웹뷰에서 토큰을 성공적으로 가져왔을 때 호출
  Future<void> processTokenFromCallback(String token) async {
    await _storeToken(token);
  }
}
