Dart 2.12 버전부터 도입된 Null Safety는 널(null) 관련 오류를 컴파일 시점에 잡아내어 런타임 에러를 방지하는 중요한 기능이다.

### Null Safety의 기본 개념

1. **Non-nullable 타입**: 기본적으로 모든 변수는 null이 될 수 없다.
   ```dart
   String name = 'Alice'; // OK
   String name = null;    // 컴파일 에러
   ```

2. **Nullable 타입**: 변수가 null을 가질 수 있음을 명시적으로 선언한다.
   ```dart
   String? nullableName = 'Bob';
   nullableName = null;   // OK
   ```

### Null 처리 연산자

1. **Null-aware 연산자 (?.)**: null이 아닐 때만 메서드나 프로퍼티에 접근한다.
   ```dart
   String? name = 'Alice';
   print(name?.length);   // 5
   name = null;
   print(name?.length);   // null
   ```

2. **Null 병합 연산자 (??)**: 좌변이 null이면 우변의 값을 사용한다.
   ```dart
   String? name;
   print(name ?? 'Guest');   // Guest
   ```

3. **Null 할당 연산자 (??=)**: 변수가 null일 때만 값을 할당한다.
   ```dart
   String? name;
   name ??= 'Guest';
   print(name);   // Guest
   ```

4. **Bang 연산자 (!)**: Null이 아님을 개발자가 보증한다. (주의 필요)
   ```dart
   String? nullableName = 'Alice';
   String nonNullableName = nullableName!;
   ```

### Late 키워드

초기화를 나중에 하겠다고 컴파일러에 알려주는 키워드이다.

```dart
late String name;
// ... 나중에
name = 'Alice';
print(name);   // Alice
```

### Null 체크

```dart
if (name != null) {
  print(name.length);
} else {
  print('Name is null');
}
```

Null Safety를 적절히 활용하면 널 포인터 예외(Null Pointer Exception)와 같은 런타임 오류를 크게 줄일 수 있다. 이는 코드의 안정성과 신뢰성을 높이는 데 큰 도움이 된다.