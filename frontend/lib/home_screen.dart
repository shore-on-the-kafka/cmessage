import 'package:flutter/material.dart';

import 'auth_service.dart';
import 'login_screen.dart';
import 'message_service.dart';
import 'user_model.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final AuthService _authService = AuthService();
  final MessageService _messageService = MessageService();
  Future<List<Message>>? _allMessagesFuture;
  User? _currentUser; // 현재 로그인한 사용자 정보를 저장할 변수

  final TextEditingController _receiverIdController = TextEditingController();
  final TextEditingController _messageContentController =
      TextEditingController();

  @override
  void initState() {
    super.initState();
    _allMessagesFuture = _fetchAllMessages();
  }

  @override
  void dispose() {
    _receiverIdController.dispose();
    _messageContentController.dispose();
    super.dispose();
  }

  // 사용자 정보를 먼저 가져온 후, 해당 사용자의 ID로 메시지를 조회하는 함수
  Future<List<Message>> _fetchAllMessages() async {
    // 1. 내 정보를 가져옵니다.
    final User me = await _authService.getMe();
    if (mounted) {
      setState(() {
        _currentUser = me; // 현재 사용자 정보 저장
      });
    }

    // 2. 받은 메시지와 보낸 메시지를 동시에 가져옵니다.
    final results = await Future.wait([
      _messageService.getReceivedMessages(me.id),
      _messageService.getSentMessages(me.id),
    ]);

    // 3. 두 목록을 합치고 시간순으로 정렬합니다 (최신 메시지가 위로).
    final allMessages = [...results[0], ...results[1]];
    allMessages.sort((a, b) => b.timestamp.compareTo(a.timestamp));

    return allMessages;
  }

  // 메시지 목록을 새로고침하는 함수
  Future<void> _handleRefresh() async {
    setState(() {
      _allMessagesFuture = _fetchAllMessages();
    });
  }

  // 로그아웃 처리
  Future<void> _handleLogout() async {
    await _authService.logout();
    if (mounted) {
      // 로그인 화면으로 이동하고 이전 화면들을 모두 제거
      Navigator.of(context).pushAndRemoveUntil(
        MaterialPageRoute(builder: (context) => const LoginScreen()),
        (Route<dynamic> route) => false,
      );
    }
  }

  // 메시지를 전송하는 함수
  Future<void> _sendMessage() async {
    if (_currentUser == null) {
      _showSnackBar('사용자 정보를 불러오지 못했습니다. 다시 로그인해주세요.', Colors.red);
      return;
    }

    final String receiverId = _receiverIdController.text.trim();
    final String content = _messageContentController.text.trim();

    if (receiverId.isEmpty || content.isEmpty) {
      _showSnackBar('받는 사람 ID와 메시지 내용을 입력해주세요.', Colors.orange);
      return;
    }

    try {
      await _messageService.sendMessage(_currentUser!.id, receiverId, content);
      _showSnackBar('메시지가 성공적으로 전송되었습니다!', Colors.green);
      _receiverIdController.clear();
      _messageContentController.clear();
      FocusScope.of(context).unfocus(); // 키보드 숨기기
      _handleRefresh(); // 메시지 전송 후 받은 메시지 목록 새로고침
    } catch (e) {
      _showSnackBar('메시지 전송 실패: ${e.toString()}', Colors.red);
    }
  }

  void _showSnackBar(String message, Color color) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: color,
        duration: const Duration(seconds: 2),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_currentUser != null
            ? '안녕하세요, ${_currentUser!.name}님'
            : '나의 메시지함'),
        actions: [
          IconButton(
            icon: const Icon(Icons.logout),
            onPressed: _handleLogout,
            tooltip: '로그아웃',
          ),
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _handleRefresh,
          ),
        ],
      ),
      body: Column(
        // Column으로 변경하여 메시지 목록과 입력 필드를 함께 배치
        children: [
          Expanded(
            // 메시지 목록이 남은 공간을 모두 차지하도록 Expanded 사용
            child: FutureBuilder<List<Message>>(
              future: _allMessagesFuture,
              builder: (context, snapshot) {
                if (snapshot.connectionState == ConnectionState.waiting) {
                  return const Center(child: CircularProgressIndicator());
                } else if (snapshot.hasData && snapshot.data!.isNotEmpty) {
                  return RefreshIndicator(
                    onRefresh: _handleRefresh,
                    child: ListView.builder(
                      itemCount: snapshot.data!.length,
                      itemBuilder: (context, index) {
                        final message = snapshot.data![index];
                        final bool isSentByMe =
                            message.senderId == _currentUser?.id;
                        return Card(
                          margin: const EdgeInsets.all(8.0),
                          color: isSentByMe ? Colors.green[50] : null,
                          child: Padding(
                            padding: const EdgeInsets.all(16.0),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  isSentByMe
                                      ? '받는 사람: ${message.receiverId}'
                                      : '보낸 사람: ${message.senderId}',
                                  style: const TextStyle(
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                                const SizedBox(height: 4),
                                Text(message.content),
                                const SizedBox(height: 4),
                                Text(
                                  '시간: ${message.timestamp.toLocal().toString().split('.')[0]}',
                                  style: const TextStyle(
                                    fontSize: 12,
                                    color: Colors.grey,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        );
                      },
                    ),
                  );
                }

                // 오류 또는 빈 목록 상태를 처리합니다.
                // 두 경우 모두 새로고침이 가능하도록 RefreshIndicator로 감쌉니다.
                Widget content;
                if (snapshot.hasError) {
                  content = Text('오류: ${snapshot.error}');
                } else {
                  content = const Text('메시지가 없습니다.');
                }

                return RefreshIndicator(
                  onRefresh: _handleRefresh,
                  child: LayoutBuilder(
                    builder: (context, constraints) {
                      return SingleChildScrollView(
                        physics: const AlwaysScrollableScrollPhysics(),
                        child: ConstrainedBox(
                          constraints: BoxConstraints(
                            minHeight: constraints.maxHeight,
                          ),
                          child: Center(child: content),
                        ),
                      );
                    },
                  ),
                );
              },
            ),
          ),
          // 메시지 전송 UI
          Padding(
            padding: const EdgeInsets.all(8.0),
            child: Column(
              children: [
                TextField(
                  controller: _receiverIdController,
                  decoration: const InputDecoration(
                    labelText: '받는 사람 ID',
                    border: OutlineInputBorder(),
                  ),
                ),
                const SizedBox(height: 8),
                TextField(
                  controller: _messageContentController,
                  decoration: const InputDecoration(
                    labelText: '메시지 내용',
                    border: OutlineInputBorder(),
                  ),
                  maxLines: 3, // 여러 줄 입력 가능하도록 설정
                ),
                const SizedBox(height: 8),
                SizedBox(
                  width: double.infinity, // 버튼을 가로로 꽉 채움
                  child: ElevatedButton(
                    onPressed: _sendMessage,
                    child: const Text('메시지 보내기'),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
