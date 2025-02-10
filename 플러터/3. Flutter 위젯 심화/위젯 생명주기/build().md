
`build()` 메서드는 Flutter 위젯의 UI를 구성하는 핵심 메서드이다.

## 주요 특징

1. **호출 시점**: 
   - 위젯이 처음 생성될 때
   - `setState()`가 호출될 때
   - 부모 위젯이 재구성될 때
   - `InheritedWidget`이 업데이트될 때

2. **목적**: 
   - 위젯의 시각적 표현을 정의한다.
   - 현재 상태에 기반하여 UI를 구성한다.

3. **반환 값**: Widget 객체를 반환해야 한다.

4. **성능**: 자주 호출될 수 있으므로 가능한 한 효율적으로 구현해야 한다.

## 사용 예시

```dart
class MyWidget extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Container(
      child: Text('Hello, Flutter!'),
    );
  }
}
```

## 주요 고려사항

- **순수성**: `build()` 메서드는 순수 함수여야 한다. 즉, 동일한 입력에 대해 항상 동일한 출력을 생성해야 한다.
- **상태 변경 금지**: `build()` 내에서 `setState()`를 호출하거나 상태를 변경해서는 안 된다.
- **컨텍스트 사용**: `BuildContext`를 통해 테마, 미디어 쿼리 등의 정보에 접근할 수 있다.
- **조건부 렌더링**: 조건에 따라 다른 위젯을 반환할 수 있다.

`build()` 메서드는 Flutter 앱의 UI를 구성하는 핵심 요소로, 효율적이고 선언적인 방식으로 사용자 인터페이스를 정의하는 데 사용된다.