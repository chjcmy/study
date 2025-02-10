
`didUpdateWidget()` 메서드는 위젯이 업데이트될 때 호출되는 State 객체의 생명주기 메서드이다.

## 주요 특징

1. **호출 시점**: 
   - 부모 위젯이 재빌드되어 이 위젯의 구성이 변경될 때 호출된다.
   - 위젯의 key나 runtimeType이 변경되지 않았을 때만 호출된다.

2. **목적**: 
   - 이전 위젯과 새 위젯을 비교하여 필요한 업데이트를 수행한다.
   - 위젯의 속성 변경에 따른 State 객체의 갱신 작업을 수행한다.

3. **매개변수**: 
   - `oldWidget`: 이전 위젯의 참조를 제공한다.

4. **super 호출**: 반드시 `super.didUpdateWidget(oldWidget)`을 호출해야 한다.

## 사용 예시

```dart
class _MyWidgetState extends State<MyWidget> {
  @override
  void didUpdateWidget(MyWidget oldWidget) {
    super.didUpdateWidget(oldWidget);
    
    if (widget.someProperty != oldWidget.someProperty) {
      // 속성 변경에 따른 처리
      _updateSomething();
    }
  }

  void _updateSomething() {
    // 업데이트 로직
  }

  @override
  Widget build(BuildContext context) {
    // 위젯 빌드
  }
}
```

## 주요 용도

- 위젯의 구성 변경에 따른 State 객체 업데이트
- 이전 위젯과 새 위젯의 속성 비교
- 리스너 재등록 또는 애니메이션 컨트롤러 재설정

## 주의사항

- 이 메서드 내에서 `setState()`를 호출할 수 있지만, 불필요한 재빌드를 피하기 위해 신중하게 사용해야 한다.
- 성능에 영향을 줄 수 있으므로, 꼭 필요한 경우에만 사용해야 한다.

`didUpdateWidget()` 메서드는 위젯의 구성 변경에 따른 State 객체의 업데이트를 관리하는 데 중요한 역할을 한다. 이를 통해 위젯의 속성 변경에 효과적으로 대응할 수 있다.