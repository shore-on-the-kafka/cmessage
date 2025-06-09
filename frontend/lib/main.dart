import 'package:flutter/material.dart';

import 'auth_service.dart';
import 'home_screen.dart';
import 'login_screen.dart';

void main() {
  // 웹뷰 초기화 (필요한 경우)
  // WidgetsFlutterBinding.ensureInitialized();
  // await InAppWebViewController.setWebContentsDebuggingEnabled(true); // 개발 중 웹뷰 디버깅 활성화

  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'CMessage Demo', // 앱 제목 변경
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.green),
        // Line 테마에 맞춰 변경 가능
        useMaterial3: true,
      ),
      home: const AuthCheck(), // 인증 상태 확인 위젯으로 시작
    );
  }
}

class AuthCheck extends StatefulWidget {
  const AuthCheck({super.key});

  @override
  State<AuthCheck> createState() => _AuthCheckState();
}

class _AuthCheckState extends State<AuthCheck> {
  final AuthService _authService = AuthService();
  Future<String?>? _tokenFuture;

  @override
  void initState() {
    super.initState();
    _tokenFuture = _authService.getToken();
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<String?>(
      future: _tokenFuture,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Scaffold(
            body: Center(child: CircularProgressIndicator()),
          ); // 로딩 중 표시
        } else if (snapshot.hasData && snapshot.data != null) {
          // 토큰이 있으면 홈 화면으로 이동
          // TODO: 여기서 토큰 유효성 검사를 백엔드에 요청할 수도 있습니다.
          return const HomeScreen();
        } else {
          // 토큰이 없으면 로그인 화면으로 이동
          return const LoginScreen();
        }
      },
    );
  }
}
