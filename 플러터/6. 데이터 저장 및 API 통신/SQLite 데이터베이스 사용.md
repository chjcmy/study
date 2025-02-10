SQLite는 Flutter에서 로컬 데이터베이스로 사용할 수 있는 경량 관계형 데이터베이스이다. SQLite를 사용하는 방법은 다음과 같다:

1. 의존성 추가:
pubspec.yaml 파일에 sqflite와 path 패키지를 추가한다.

```yaml
dependencies:
  sqflite: ^2.0.0+4
  path: ^1.8.0
```

2. 데이터베이스 헬퍼 클래스 생성:

```dart
import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';

class DatabaseHelper {
  static final DatabaseHelper instance = DatabaseHelper._init();
  static Database? _database;

  DatabaseHelper._init();

  Future<Database> get database async {
    if (_database != null) return _database!;
    _database = await _initDB('my_database.db');
    return _database!;
  }

  Future<Database> _initDB(String filePath) async {
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, filePath);

    return await openDatabase(path, version: 1, onCreate: _createDB);
  }

  Future _createDB(Database db, int version) async {
    await db.execute('''
      CREATE TABLE users(
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT,
        age INTEGER
      )
    ''');
  }

  Future<int> insertUser(User user) async {
    final db = await database;
    return await db.insert('users', user.toMap());
  }

  Future<List<User>> getUsers() async {
    final db = await database;
    final List<Map<String, dynamic>> maps = await db.query('users');
    return List.generate(maps.length, (i) {
      return User(
        id: maps[i]['id'],
        name: maps[i]['name'],
        age: maps[i]['age'],
      );
    });
  }

  Future<int> updateUser(User user) async {
    final db = await database;
    return await db.update(
      'users',
      user.toMap(),
      where: 'id = ?',
      whereArgs: [user.id],
    );
  }

  Future<int> deleteUser(int id) async {
    final db = await database;
    return await db.delete(
      'users',
      where: 'id = ?',
      whereArgs: [id],
    );
  }
}
```

1. User 모델 클래스 생성:

```dart
class User {
  final int? id;
  final String name;
  final int age;

  User({this.id, required this.name, required this.age});

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'name': name,
      'age': age,
    };
  }
}
```

2. 데이터베이스 사용:

```dart
final dbHelper = DatabaseHelper.instance;

// 사용자 추가
Future<void> addUser() async {
  User newUser = User(name: 'John Doe', age: 30);
  int id = await dbHelper.insertUser(newUser);
  print('inserted row id: $id');
}

// 모든 사용자 조회
Future<void> getUsers() async {
  List<User> users = await dbHelper.getUsers();
  users.forEach((user) {
    print('User: ${user.name}, Age: ${user.age}');
  });
}

// 사용자 업데이트
Future<void> updateUser() async {
  User user = User(id: 1, name: 'Jane Doe', age: 31);
  int rowsAffected = await dbHelper.updateUser(user);
  print('updated $rowsAffected row(s)');
}

// 사용자 삭제
Future<void> deleteUser() async {
  int rowsAffected = await dbHelper.deleteUser(1);
  print('deleted $rowsAffected row(s)');
}
```

이러한 방식으로 SQLite 데이터베이스를 사용하여 로컬에 데이터를 저장하고 관리할 수 있다. SQLite는 복잡한 쿼리와 대량의 데이터를 처리하는 데 적합하며, 앱의 오프라인 기능을 구현하는 데 유용하다.