import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';
import 'create_group_screen.dart';
import 'group_chat_screen.dart';
import 'group_service.dart';

class GroupListScreen extends StatefulWidget {
  const GroupListScreen({super.key});

  @override
  State<GroupListScreen> createState() => _GroupListScreenState();
}

class _GroupListScreenState extends State<GroupListScreen> {
  final GroupService _groupService = GroupService();
  List<Map<String, dynamic>> _groups = [];
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _loadGroups();
  }

  // 로컬 저장소에서 그룹 목록 로드
  Future<void> _loadGroups() async {
    setState(() {
      _isLoading = true;
    });

    try {
      final prefs = await SharedPreferences.getInstance();
      final String? groupsJson = prefs.getString('user_groups');
      if (groupsJson != null) {
        final List<dynamic> groupsList = jsonDecode(groupsJson);
        setState(() {
          _groups = groupsList.cast<Map<String, dynamic>>();
        });
      }
    } catch (e) {
      print('그룹 목록 로드 중 오류: $e');
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  // 로컬 저장소에 그룹 목록 저장
  Future<void> _saveGroups() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final String groupsJson = jsonEncode(_groups);
      await prefs.setString('user_groups', groupsJson);
    } catch (e) {
      print('그룹 목록 저장 중 오류: $e');
    }
  }

  // 그룹 추가 (로컬 저장소에)
  Future<void> _addGroup(Map<String, dynamic> group) async {
    setState(() {
      _groups.add(group);
    });
    await _saveGroups();
  }

  // 그룹 생성 화면으로 이동
  Future<void> _navigateToCreateGroup() async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => const CreateGroupScreen(),
      ),
    );

    if (result != null) {
      // 생성된 그룹 정보를 백엔드에서 가져와서 로컬에 저장
      try {
        final String groupId = result['groupId'];
        final groupInfo = await _groupService.getGroup(groupId);
        if (groupInfo != null) {
          await _addGroup(groupInfo);
        }
      } catch (e) {
        print('그룹 정보 가져오기 실패: $e');
        // 실패해도 기본 정보로 추가
        await _addGroup({
          'id': result['groupId'],
          'name': '새 그룹',
          'members': [],
        });
      }
    }
  }

  // 그룹 채팅 화면으로 이동
  void _navigateToGroupChat(Map<String, dynamic> group) {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => GroupChatScreen(
          groupId: group['id'],
          groupName: group['name'],
        ),
      ),
    );
  }

  // 그룹 직접 입력으로 참여
  Future<void> _joinGroupById() async {
    final TextEditingController controller = TextEditingController();
    
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('그룹 참여'),
        content: TextField(
          controller: controller,
          decoration: const InputDecoration(
            labelText: '그룹 ID',
            hintText: '참여할 그룹 ID를 입력하세요',
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('취소'),
          ),
          TextButton(
            onPressed: () async {
              final groupId = controller.text.trim();
              if (groupId.isNotEmpty) {
                Navigator.pop(context);
                
                // 그룹 정보 가져오기
                try {
                  final groupInfo = await _groupService.getGroup(groupId);
                  if (groupInfo != null) {
                    await _addGroup(groupInfo);
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('그룹에 참여했습니다')),
                    );
                  } else {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('그룹을 찾을 수 없습니다')),
                    );
                  }
                } catch (e) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text('그룹 참여 중 오류: $e')),
                  );
                }
              }
            },
            child: const Text('참여'),
          ),
        ],
      ),
    );
  }

  // 그룹 삭제 (로컬에서만)
  Future<void> _removeGroup(int index) async {
    final group = _groups[index];
    
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('그룹 삭제'),
        content: Text('\'${group['name']}\' 그룹을 목록에서 제거하시겠습니까?\n(실제 그룹은 삭제되지 않습니다)'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('취소'),
          ),
          TextButton(
            onPressed: () async {
              Navigator.pop(context);
              setState(() {
                _groups.removeAt(index);
              });
              await _saveGroups();
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('그룹이 목록에서 제거되었습니다')),
              );
            },
            child: const Text('삭제'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('그룹 채팅'),
        backgroundColor: Colors.green,
        foregroundColor: Colors.white,
        actions: [
          IconButton(
            icon: const Icon(Icons.person_add),
            onPressed: _joinGroupById,
            tooltip: '그룹 ID로 참여',
          ),
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _loadGroups,
            tooltip: '새로고침',
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _groups.isEmpty
              ? const Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(
                        Icons.group_outlined,
                        size: 64,
                        color: Colors.grey,
                      ),
                      SizedBox(height: 16),
                      Text(
                        '참여한 그룹이 없습니다',
                        style: TextStyle(
                          fontSize: 18,
                          color: Colors.grey,
                        ),
                      ),
                      SizedBox(height: 8),
                      Text(
                        '새 그룹을 만들거나 그룹 ID로 참여해보세요',
                        style: TextStyle(
                          fontSize: 14,
                          color: Colors.grey,
                        ),
                      ),
                    ],
                  ),
                )
              : ListView.builder(
                  itemCount: _groups.length,
                  itemBuilder: (context, index) {
                    final group = _groups[index];
                    final String groupName = group['name'] ?? '이름 없음';
                    final List<dynamic> members = group['members'] ?? [];
                    
                    return Card(
                      margin: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      child: ListTile(
                        leading: CircleAvatar(
                          backgroundColor: Colors.green,
                          child: Text(
                            groupName.isNotEmpty ? groupName[0].toUpperCase() : 'G',
                            style: const TextStyle(
                              color: Colors.white,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                        title: Text(
                          groupName,
                          style: const TextStyle(fontWeight: FontWeight.bold),
                        ),
                        subtitle: Text('${members.length}명'),
                        trailing: IconButton(
                          icon: const Icon(Icons.more_vert),
                          onPressed: () {
                            showModalBottomSheet(
                              context: context,
                              builder: (context) => Column(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  ListTile(
                                    leading: const Icon(Icons.info),
                                    title: const Text('그룹 정보'),
                                    onTap: () {
                                      Navigator.pop(context);
                                      showDialog(
                                        context: context,
                                        builder: (context) => AlertDialog(
                                          title: Text(groupName),
                                          content: Column(
                                            mainAxisSize: MainAxisSize.min,
                                            crossAxisAlignment: CrossAxisAlignment.start,
                                            children: [
                                              Text('그룹 ID: ${group['id']}'),
                                              const SizedBox(height: 10),
                                              const Text('멤버:'),
                                              ...members.map((member) => Text('• $member')),
                                            ],
                                          ),
                                          actions: [
                                            TextButton(
                                              onPressed: () => Navigator.pop(context),
                                              child: const Text('확인'),
                                            ),
                                          ],
                                        ),
                                      );
                                    },
                                  ),
                                  ListTile(
                                    leading: const Icon(Icons.delete, color: Colors.red),
                                    title: const Text('목록에서 제거'),
                                    onTap: () {
                                      Navigator.pop(context);
                                      _removeGroup(index);
                                    },
                                  ),
                                ],
                              ),
                            );
                          },
                        ),
                        onTap: () => _navigateToGroupChat(group),
                      ),
                    );
                  },
                ),
      floatingActionButton: FloatingActionButton(
        onPressed: _navigateToCreateGroup,
        backgroundColor: Colors.green,
        child: const Icon(Icons.add, color: Colors.white),
      ),
    );
  }
}