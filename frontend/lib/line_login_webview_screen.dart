import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';

import 'auth_service.dart'; // AuthService 임포트

class LineLoginWebViewScreen extends StatefulWidget {
  final String initialUrl; // Line 로그인 시작 URL
  final AuthService authService;

  const LineLoginWebViewScreen({
    super.key,
    required this.initialUrl,
    required this.authService,
  });

  @override
  State<LineLoginWebViewScreen> createState() => _LineLoginWebViewScreenState();
}

class _LineLoginWebViewScreenState extends State<LineLoginWebViewScreen> {
  InAppWebViewController? _webViewController;
  bool _isLoading = true;
  String? _loadingError;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Line으로 로그인'),
        leading: IconButton(
          icon: const Icon(Icons.close),
          onPressed: () => Navigator.of(context).pop(null), // 토큰 없이 닫기
        ),
      ),
      body: Stack(
        children: [
          InAppWebView(
            initialUrlRequest: URLRequest(url: WebUri(widget.initialUrl)),
            initialSettings: InAppWebViewSettings(
              javaScriptEnabled: true,
              // Android에서 cleartext 트래픽 허용 (개발 중 HTTP URL 사용 시)
              // 프로덕션에서는 HTTPS를 사용하고 이 옵션은 false 또는 제거
              // usesCleartextTraffic: true,
            ),
            onWebViewCreated: (controller) {
              _webViewController = controller;
            },
            onLoadStart: (controller, url) {
              setState(() {
                _isLoading = true;
                _loadingError = null;
              });
              debugPrint("WebView onLoadStart: $url");

              // 백엔드의 최종 콜백 URL 감지 (토큰을 포함한 JSON 응답을 기대)
              // AuthController.kt의 /login/oauth2/line 경로를 확인
              if (url != null && url.toString().startsWith(widget.authService.backendLineCallbackBaseUrl)) {
                // 이 시점에서 페이지 로드가 완료된 후 내용을 가져와야 함
                // onLoadStop에서 처리하는 것이 더 안정적일 수 있음
              }
            },
            onLoadStop: (controller, url) async {
              setState(() {
                _isLoading = false;
              });
              debugPrint("WebView onLoadStop: $url");

              if (url != null && url.toString().startsWith(widget.authService.backendLineCallbackBaseUrl)) {
                // 페이지 로드가 완료되었으므로, 페이지의 내용을 가져와 JSON 파싱 시도
                // 백엔드는 LoginSuccessResponse(token) 형태의 JSON을 반환함
                try {
                  // JavaScript를 실행하여 body의 innerText (JSON 문자열)를 가져옴
                  final pageBody = await controller.evaluateJavascript(source: "document.body.innerText");
                  if (pageBody != null && pageBody.toString().isNotEmpty) {
                    debugPrint("Page body from callback: $pageBody");
                    final jsonData = jsonDecode(pageBody.toString());
                    if (jsonData is Map<String, dynamic> && jsonData.containsKey('token')) {
                      final token = jsonData['token'] as String;
                      debugPrint("Token extracted: $token");
                      // ignore: use_build_context_synchronously
                      Navigator.of(context).pop(token); // 토큰 반환하며 웹뷰 닫기
                      return;
                    } else {
                      debugPrint("Token not found in JSON response or invalid format.");
                      setState(() {
                        _loadingError = "로그인 응답에서 토큰을 찾을 수 없습니다.";
                      });
                    }
                  } else {
                    debugPrint("Page body is empty or null from callback URL.");
                    setState(() {
                      _loadingError = "로그인 응답이 비어있습니다.";
                    });
                  }
                } catch (e) {
                  debugPrint("Error parsing token from callback: $e");
                  setState(() {
                    _loadingError = "로그인 응답 처리 중 오류: $e";
                  });
                }
              }
            },
            onReceivedError: (controller, request, error) {
              setState(() {
                _isLoading = false;
                _loadingError = "웹 페이지 로드 오류: ${error.description}";
              });
              debugPrint("WebView onReceivedError: ${error.description}");
            },
            onReceivedHttpError: (controller, request, errorResponse) {
              setState(() {
                _isLoading = false;
                _loadingError = "HTTP 오류: ${errorResponse.statusCode}";
              });
              debugPrint("WebView onReceivedHttpError: ${errorResponse.statusCode}");
            },

          ),
          if (_isLoading)
            const Center(child: CircularProgressIndicator()),
          if (_loadingError != null)
            Center(
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Text(
                  _loadingError!,
                  style: const TextStyle(color: Colors.red, fontSize: 16),
                  textAlign: TextAlign.center,
                ),
              ),
            ),
        ],
      ),
    );
  }
}
