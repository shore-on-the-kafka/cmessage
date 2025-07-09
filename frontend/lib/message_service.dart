import 'dart:convert';

import 'package:http/http.dart' as http;
import 'auth_service.dart'; // AuthService를 임포트합니다.

// 백엔드의 Message 도메인 객체에 상응하는 Dart 모델
class Message {
  final String id;
  final String senderId;
  final String receiverId;
  final String content;
  final DateTime timestamp;

  Message({
    required this.id,
    required this.senderId,
    required this.receiverId,
    required this.content,
    required this.timestamp,
  });

  factory Message.fromJson(Map<String, dynamic> json) {
    return Message(
      id: json['messageId'],
      senderId: json['senderId'],
      receiverId: json['receiverId'],
      content: json['content'],
      timestamp: DateTime.parse(json['timestamp']), // ISO 8601 문자열 파싱
    );
  }
}

class MessageService {
  final String _baseUrl = 'http://localhost:8080/api/v1'; // 실제 백엔드 URL로 변경 필요
  final AuthService _authService = AuthService(); // AuthService 인스턴스를 사용합니다.

  Future<String?> _getToken() async {
    // AuthService를 통해 현재 메모리에 있는 토큰을 가져옵니다.
    return await _authService.getToken();
  }

  // receiverId를 파라미터로 받도록 수정합니다.
  Future<List<Message>> getReceivedMessages(String receiverId) async {
    final token = await _getToken();
    if (token == null) {
      throw Exception('Authentication token not found.');
    }
    final response = await http.get(
      Uri.parse('$_baseUrl/messages?receiverId=$receiverId'),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $token',
      },
    );
    if (response.statusCode == 200) {
      List<dynamic> messageJson = json.decode(utf8.decode(response.bodyBytes));
      return messageJson.map((json) => Message.fromJson(json)).toList();
    } else {
      throw Exception(
        'Failed to load received messages: ${response.statusCode} ${response.body}',
      );
    }
  }

  // senderId를 파라미터로 받아서 보낸 메시지를 조회합니다.
  Future<List<Message>> getSentMessages(String senderId) async {
    final token = await _getToken();
    if (token == null) {
      throw Exception('Authentication token not found.');
    }
    final response = await http.get(
      Uri.parse('$_baseUrl/messages?senderId=$senderId'),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $token',
      },
    );
    if (response.statusCode == 200) {
      List<dynamic> messageJson = json.decode(utf8.decode(response.bodyBytes));
      return messageJson.map((json) => Message.fromJson(json)).toList();
    } else {
      throw Exception(
        'Failed to load sent messages: ${response.statusCode} ${response.body}',
      );
    }
  }

  // 메시지를 전송하는 함수 추가
  Future<void> sendMessage(
    String senderId,
    String receiverId,
    String content,
  ) async {
    final token = await _getToken();
    if (token == null) {
      throw Exception('Authentication token not found.');
    }

    final response = await http.post(
      Uri.parse('$_baseUrl/messages'),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $token',
      },
      body: json.encode({
        'senderId': senderId,
        'receiverId': receiverId,
        'content': content,
      }),
    );

    if (response.statusCode != 201) {
      // 201 Created
      throw Exception(
        'Failed to send message: ${response.statusCode} ${response.body}',
      );
    }
  }
}
