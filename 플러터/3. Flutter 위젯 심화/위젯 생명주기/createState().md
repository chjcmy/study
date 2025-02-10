# createState()

`createState()` 메서드는 StatefulWidget의 생명주기에서 가장 먼저 호출되는 메서드이다.

## 주요 특징

1. **호출 시점**: StatefulWidget이 위젯 트리에 삽입될 때 Flutter 프레임워크에 의해 자동으로 호출된다.

2. **목적**: 이 위젯과 연관된 State 객체를 생성한다.

3. **반환 값**: State 클래스의 인스턴스를 반환해야 한다.

4. **오버라이드**: StatefulWidget 클래스에서 이 메서드를 반드시 오버라이드해야 한다.

## 사용 예시

```dart
class MyWidget extends StatefulWidget {
  @override
  _MyWidgetState createState() => _MyWidgetState();
}

class _MyWidgetState extends State<MyWidget> {
  // State 클래스 구현
}
```

## 주의사항

- `createState()`는 위젯이 생성될 때 한 번만 호출된다.
- 이 메서드 내에서는 State 객체를 생성만 해야 하며, 초기화 작업은 `initState()`에서 수행해야 한다.
- State 객체는 위젯의 전체 수명 동안 유지되므로, 여기서 생성된 State 객체는 위젯이 제거될 때까지 존재한다.

`createState()` 메서드는 StatefulWidget과 State 객체를 연결하는 중요한 역할을 한다. 이를 통해 Flutter는 위젯의 상태를 관리하고 업데이트할 수 있게 된다.