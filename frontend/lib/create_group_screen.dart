import 'package:flutter/material.dart';
import 'group_service.dart';

class CreateGroupScreen extends StatefulWidget {
  const CreateGroupScreen({super.key});

  @override
  State<CreateGroupScreen> createState() => _CreateGroupScreenState();
}

class _CreateGroupScreenState extends State<CreateGroupScreen> {
  final TextEditingController _groupNameController = TextEditingController();
  final TextEditingController _memberController = TextEditingController();
  final GroupService _groupService = GroupService();
  
  final List<String> _members = [];
  bool _isLoading = false;

  @override
  void dispose() {
    _groupNameController.dispose();
    _memberController.dispose();
    super.dispose();
  }

  void _addMember() {
    String memberId = _memberController.text.trim();
    if (memberId.isNotEmpty && !_members.contains(memberId)) {
      setState(() {
        _members.add(memberId);
      });
      _memberController.clear();
    }
  }

  void _removeMember(String memberId) {
    setState(() {
      _members.remove(memberId);
    });
  }

  Future<void> _createGroup() async {
    String groupName = _groupNameController.text.trim();
    if (groupName.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('그룹 이름을 입력해주세요')),
      );
      return;
    }

    setState(() {
      _isLoading = true;
    });

    try {
      final result = await _groupService.createGroup(groupName, _members);
      if (result != null) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('그룹이 생성되었습니다')),
        );
        Navigator.pop(context, result); // 생성된 그룹 정보를 반환
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('그룹 생성에 실패했습니다')),
        );
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('오류가 발생했습니다: $e')),
      );
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('그룹 생성'),
        backgroundColor: Colors.green,
        foregroundColor: Colors.white,
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 그룹 이름 입력
            TextField(
              controller: _groupNameController,
              decoration: const InputDecoration(
                labelText: '그룹 이름',
                border: OutlineInputBorder(),
                prefixIcon: Icon(Icons.group),
              ),
            ),
            const SizedBox(height: 20),
            
            // 멤버 추가 섹션
            const Text(
              '멤버 추가',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 10),
            
            // 멤버 ID 입력
            Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _memberController,
                    decoration: const InputDecoration(
                      labelText: '멤버 ID',
                      border: OutlineInputBorder(),
                      prefixIcon: Icon(Icons.person_add),
                    ),
                    onSubmitted: (_) => _addMember(),
                  ),
                ),
                const SizedBox(width: 10),
                ElevatedButton(
                  onPressed: _addMember,
                  child: const Text('추가'),
                ),
              ],
            ),
            const SizedBox(height: 20),
            
            // 멤버 리스트
            const Text(
              '그룹 멤버',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 10),
            
            Expanded(
              child: _members.isEmpty
                  ? const Center(
                      child: Text(
                        '추가된 멤버가 없습니다\n(본인은 자동으로 추가됩니다)',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          color: Colors.grey,
                          fontSize: 14,
                        ),
                      ),
                    )
                  : ListView.builder(
                      itemCount: _members.length,
                      itemBuilder: (context, index) {
                        final member = _members[index];
                        return Card(
                          child: ListTile(
                            leading: const Icon(Icons.person),
                            title: Text(member),
                            trailing: IconButton(
                              icon: const Icon(Icons.remove_circle, color: Colors.red),
                              onPressed: () => _removeMember(member),
                            ),
                          ),
                        );
                      },
                    ),
            ),
            
            const SizedBox(height: 20),
            
            // 그룹 생성 버튼
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: _isLoading ? null : _createGroup,
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.green,
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.all(16),
                ),
                child: _isLoading
                    ? const CircularProgressIndicator(color: Colors.white)
                    : const Text(
                        '그룹 생성',
                        style: TextStyle(fontSize: 16),
                      ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}