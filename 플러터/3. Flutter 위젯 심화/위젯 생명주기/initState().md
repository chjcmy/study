# initState()

`initState()` 메서드는 State 객체의 생명주기에서 가장 먼저 호출되는 메서드이다.

## 주요 특징

1. **호출 시점**: State 객체가 위젯 트리에 삽입된 직후에 단 한 번만 호출된다.

2. **목적**: State 객체의 초기화 작업을 수행한다.

3. **super 호출**: 반드시 `super.initState()`를 호출해야 한다.

4. **비동기 작업**: 이 메서드 내에서 직접적인 비동기 작업은 수행할 수 없다.

## 사용 예시

```dart
class _MyWidgetState extends State<MyWidget> {
  late String _data;

  @override
  void initState() {
    super.initState();
    _data = 'Initial Data';
    // 초기화 작업 수행
    _loadData();
  }

  Future<void> _loadData() async {
    // 비동기 데이터 로딩
  }

  @override
  Widget build(BuildContext context) {
    // 위젯 빌드
  }
}
```

## 주요 용도

- 변수 초기화
- 리스너 등록
- 애니메이션 컨트롤러 초기화
- API 호출 시작

## 주의사항

- `BuildContext`를 사용하는 작업은 `initState()` 내에서 직접 수행할 수 없다. 대신 `addPostFrameCallback`을 사용할 수 있다.
- 상태 변경(`setState()` 호출)은 `initState()` 내에서 직접 수행해서는 안 된다.

`initState()` 메서드는 State 객체의 초기 설정을 위한 중요한 위치이며, 위젯의 생명주기 동안 한 번만 실행되므로 효율적인 초기화 작업에 적합하다.