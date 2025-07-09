class User {
  final String id;
  final String name;

  User({required this.id, required this.name});

  factory User.fromJson(Map<String, dynamic> json) {
    // 백엔드의 UserView 응답 형식에 맞춰 파싱합니다.
    return User(id: json['id'], name: json['username']);
  }
}
