
클래스는 객체를 생성하기 위한 템플릿이다. Dart에서 클래스는 객체 지향 프로그래밍의 기본 단위로 사용된다.

## 클래스의 정의

Dart에서 클래스를 정의하는 기본 구조는 다음과 같다:

```dart
class ClassName {
  // 필드 (속성)
  // 생성자
  // 메서드
}
```

## 클래스의 구성 요소

### 필드 (Fields)

클래스의 속성을 나타내는 변수들이다. 예를 들면:

```dart
class Person {
  String name;
  int age;
}
```

### 생성자 (Constructors)

객체를 초기화하는 특별한 메서드이다. 기본 생성자는 다음과 같이 정의한다:

```dart
class Person {
  String name;
  int age;

  Person(this.name, this.age);
}
```

### 메서드 (Methods)

클래스의 행동을 정의하는 함수들이다. 예를 들면:

```dart
class Person {
  String name;
  int age;

  Person(this.name, this.age);

  void introduce() {
    print('My name is $name and I am $age years old.');
  }
}
```

## 클래스의 사용

클래스를 사용하여 객체를 생성하고 사용하는 방법은 다음과 같다:

```dart
void main() {
  var person = Person('Alice', 30);
  person.introduce();
}
```

이렇게 클래스를 사용하면 관련된 데이터와 기능을 하나의 단위로 묶어 코드의 구조화와 재사용성을 높일 수 있다.