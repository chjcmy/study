JSON 직렬화 및 역직렬화는 Flutter에서 데이터를 JSON 형식으로 변환하거나 JSON 데이터를 Dart 객체로 변환하는 과정이다. 이 과정은 주로 API 통신이나 데이터 저장 시 사용된다.

1. JSON 직렬화 (Dart 객체 → JSON):

```dart
import 'dart:convert';

class User {
  final String name;
  final int age;

  User(this.name, this.age);

  Map<String, dynamic> toJson() => {
    'name': name,
    'age': age,
  };
}

void main() {
  final user = User('John Doe', 30);
  final jsonString = jsonEncode(user.toJson());
  print(jsonString); // {"name":"John Doe","age":30}
}
```

2. JSON 역직렬화 (JSON → Dart 객체):

```dart
class User {
  final String name;
  final int age;

  User(this.name, this.age);

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      json['name'] as String,
      json['age'] as int,
    );
  }
}

void main() {
  final jsonString = '{"name":"John Doe","age":30}';
  final jsonMap = jsonDecode(jsonString);
  final user = User.fromJson(jsonMap);
  print('${user.name}, ${user.age}'); // John Doe, 30
}
```

1. 복잡한 객체의 직렬화 및 역직렬화:

```dart
class Address {
  final String street;
  final String city;

  Address(this.street, this.city);

  Map<String, dynamic> toJson() => {
    'street': street,
    'city': city,
  };

  factory Address.fromJson(Map<String, dynamic> json) {
    return Address(
      json['street'] as String,
      json['city'] as String,
    );
  }
}

class User {
  final String name;
  final int age;
  final Address address;

  User(this.name, this.age, this.address);

  Map<String, dynamic> toJson() => {
    'name': name,
    'age': age,
    'address': address.toJson(),
  };

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      json['name'] as String,
      json['age'] as int,
      Address.fromJson(json['address'] as Map<String, dynamic>),
    );
  }
}
```

이러한 방식으로 JSON 직렬화 및 역직렬화를 구현하여 데이터를 효율적으로 처리할 수 있다.