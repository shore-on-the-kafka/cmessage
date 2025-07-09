import 'dart:convert';
import 'package:http/http.dart' as http;
import 'auth_service.dart';

class GroupService {
  static const String baseUrl = 'http://localhost:8080/api/v1';
  final AuthService _authService = AuthService();

  // 그룹 생성
  Future<Map<String, dynamic>?> createGroup(String name, List<String> members) async {
    final token = await _authService.getToken();
    if (token == null) return null;

    try {
      final response = await http.post(
        Uri.parse('$baseUrl/groups'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
        body: jsonEncode({
          'name': name,
          'members': members,
        }),
      );

      if (response.statusCode == 201) {
        return jsonDecode(response.body);
      } else {
        print('그룹 생성 실패: ${response.statusCode}');
        print('응답 본문: ${response.body}');
        return null;
      }
    } catch (e) {
      print('그룹 생성 중 오류 발생: $e');
      return null;
    }
  }

  // 그룹 정보 조회
  Future<Map<String, dynamic>?> getGroup(String groupId) async {
    final token = await _authService.getToken();
    if (token == null) return null;

    try {
      final response = await http.get(
        Uri.parse('$baseUrl/groups/$groupId'),
        headers: {
          'Authorization': 'Bearer $token',
        },
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body);
      } else {
        print('그룹 정보 조회 실패: ${response.statusCode}');
        return null;
      }
    } catch (e) {
      print('그룹 정보 조회 중 오류 발생: $e');
      return null;
    }
  }

  // 그룹 멤버 추가
  Future<bool> addMembers(String groupId, List<String> userIds) async {
    final token = await _authService.getToken();
    if (token == null) return false;

    try {
      final response = await http.post(
        Uri.parse('$baseUrl/groups/$groupId/members'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
        body: jsonEncode({
          'users': userIds,
        }),
      );

      if (response.statusCode == 201) {
        return true;
      } else {
        print('그룹 멤버 추가 실패: ${response.statusCode}');
        return false;
      }
    } catch (e) {
      print('그룹 멤버 추가 중 오류 발생: $e');
      return false;
    }
  }

  // 그룹 멤버 조회
  Future<List<String>?> getGroupMembers(String groupId) async {
    final token = await _authService.getToken();
    if (token == null) return null;

    try {
      final response = await http.get(
        Uri.parse('$baseUrl/groups/$groupId/members'),
        headers: {
          'Authorization': 'Bearer $token',
        },
      );

      if (response.statusCode == 200) {
        final List<dynamic> members = jsonDecode(response.body);
        return members.cast<String>();
      } else {
        print('그룹 멤버 조회 실패: ${response.statusCode}');
        return null;
      }
    } catch (e) {
      print('그룹 멤버 조회 중 오류 발생: $e');
      return null;
    }
  }

  // 그룹 메시지 전송
  Future<Map<String, dynamic>?> sendGroupMessage(String groupId, String content) async {
    final token = await _authService.getToken();
    if (token == null) return null;

    try {
      final response = await http.post(
        Uri.parse('$baseUrl/messages'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
        body: jsonEncode({
          'groupId': groupId,
          'content': content,
        }),
      );

      if (response.statusCode == 201) {
        return jsonDecode(response.body);
      } else {
        print('그룹 메시지 전송 실패: ${response.statusCode}');
        print('응답 본문: ${response.body}');
        return null;
      }
    } catch (e) {
      print('그룹 메시지 전송 중 오류 발생: $e');
      return null;
    }
  }

  // 그룹 메시지 조회
  Future<List<Map<String, dynamic>>?> getGroupMessages(String groupId) async {
    final token = await _authService.getToken();
    if (token == null) return null;

    try {
      final response = await http.get(
        Uri.parse('$baseUrl/messages?groupId=$groupId'),
        headers: {
          'Authorization': 'Bearer $token',
        },
      );

      if (response.statusCode == 200) {
        final List<dynamic> messages = jsonDecode(response.body);
        return messages.cast<Map<String, dynamic>>();
      } else {
        print('그룹 메시지 조회 실패: ${response.statusCode}');
        return null;
      }
    } catch (e) {
      print('그룹 메시지 조회 중 오류 발생: $e');
      return null;
    }
  }
}