import 'package:flutter/material.dart';

import 'auth_service.dart';
import 'home_screen.dart'; // 로그인 성공 시 이동할 홈 화면
import 'line_login_webview_screen.dart'; // Line 로그인 웹뷰 화면

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final AuthService _authService = AuthService();
  // final _formKey = GlobalKey<FormState>(); // 일반 로그인용
  // String _email = ''; // 일반 로그인용
  // String _password = ''; // 일반 로그인용
  bool _isLoading = false;

  void _showErrorDialog(String message) {
    if (!mounted) return;
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('오류 발생'),
        content: Text(message),
        actions: <Widget>[
          TextButton(
            child: const Text('확인'),
            onPressed: () {
              Navigator.of(ctx).pop();
            },
          )
        ],
      ),
    );
  }

  Future<void> _handleLineSignIn() async {
    setState(() {
      _isLoading = true;
    });

    try {
      final lineLoginUrl = await _authService.getLineLoginInitiateUrl();
      if (lineLoginUrl != null && mounted) {
        // 웹뷰를 표시하고 결과를 기다림 (토큰 또는 null)
        final String? token = await Navigator.of(context).push<String?>(
          MaterialPageRoute(
            builder: (context) => LineLoginWebViewScreen(
              initialUrl: lineLoginUrl,
              authService: _authService,
            ),
          ),
        );

        if (token != null) {
          // 웹뷰에서 토큰을 성공적으로 받아옴
          await _authService.processTokenFromCallback(token);
          if (mounted) {
            Navigator.of(context).pushReplacement(
              MaterialPageRoute(builder: (context) => const HomeScreen()),
            );
          }
        } else if (mounted) {
          // 사용자가 웹뷰를 닫았거나 토큰을 받지 못함
          _showErrorDialog('Line 로그인이 취소되었거나 실패했습니다.');
        }
      } else if (mounted) {
        _showErrorDialog('Line 로그인 URL을 가져오지 못했습니다. 서버 연결을 확인해주세요.');
      }
    } catch (e) {
      if (mounted) {
        _showErrorDialog('Line 로그인 중 오류가 발생했습니다: $e');
      }
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('로그인')),
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              // 일반 로그인 UI (필요시 추가)
              // TextFormField(...)
              // TextFormField(...)
              // ElevatedButton(onPressed: _submitLogin, child: Text('일반 로그인')),

              const SizedBox(height: 20),
              if (_isLoading)
                const CircularProgressIndicator()
              else
                ElevatedButton.icon(
                  icon: const Icon(Icons.chat_bubble), // Line 아이콘으로 변경 가능
                  label: const Text('Line 계정으로 로그인'),
                  onPressed: _handleLineSignIn,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF00B900), // Line 녹색
                    foregroundColor: Colors.white,
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}
