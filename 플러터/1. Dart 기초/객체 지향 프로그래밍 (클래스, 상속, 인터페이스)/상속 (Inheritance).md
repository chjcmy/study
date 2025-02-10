
상속은 객체 지향 프로그래밍의 핵심 개념 중 하나로, 기존 클래스의 특성을 새로운 클래스에서 재사용할 수 있게 한다. Dart에서 상속은 `extends` 키워드를 사용하여 구현한다.

### 기본 상속

```dart
class Animal {
  void breathe() {
    print('Breathing');
  }
}

class Dog extends Animal {
  void bark() {
    print('Woof!');
  }
}

var dog = Dog();
dog.breathe(); // 출력: Breathing
dog.bark();    // 출력: Woof!
```

이 예에서 `Dog` 클래스는 `Animal` 클래스를 상속받아 `breathe` 메서드를 사용할 수 있다.

### 메서드 오버라이딩

자식 클래스에서 부모 클래스의 메서드를 재정의할 수 있다. 이를 메서드 오버라이딩이라고 한다.

```dart
class Animal {
  void makeSound() {
    print('Some generic sound');
  }
}

class Cat extends Animal {
  @override
  void makeSound() {
    print('Meow');
  }
}
```

`@override` 어노테이션은 선택사항이지만, 코드의 가독성을 높이고 실수를 방지하는 데 도움이 된다.

### super 키워드

`super` 키워드를 사용하여 부모 클래스의 생성자나 메서드를 호출할 수 있다.

```dart
class Person {
  String name;

  Person(this.name);

  void introduce() {
    print('My name is $name');
  }
}

class Student extends Person {
  String school;

  Student(String name, this.school) : super(name);

  @override
  void introduce() {
    super.introduce();
    print('I study at $school');
  }
}
```

### 상속의 제한

Dart에서는 단일 상속만을 지원한다. 즉, 한 클래스는 오직 하나의 부모 클래스만을 가질 수 있다. 그러나 이러한 제한은 인터페이스와 믹스인을 통해 보완할 수 있다.

상속을 통해 코드의 재사용성을 높이고 클래스 간의 계층 구조를 만들 수 있다. 그러나 과도한 상속은 코드를 복잡하게 만들 수 있으므로, 적절히 사용하는 것이 중요하다.