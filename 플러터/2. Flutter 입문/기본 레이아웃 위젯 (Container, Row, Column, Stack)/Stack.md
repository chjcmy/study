Stack은 Flutter에서 여러 자식 위젯을 겹쳐서 배치할 수 있게 해주는 레이아웃 위젯이다. 주요 특징은 다음과 같다:

1. 자식 위젯들을 z-축으로 쌓아올릴 수 있다.

2. Positioned 위젯을 사용하여 자식 위젯의 위치를 정확히 지정할 수 있다.

3. 비위치 지정 자식들은 alignment 속성에 따라 정렬된다.

Stack의 주요 속성들:

- children: 쌓을 자식 위젯들의 리스트
- alignment: 비위치 지정 자식들의 정렬 방식
- fit: 비위치 지정 자식들의 크기 조정 방식
- clipBehavior: 자식 위젯이 Stack의 경계를 벗어날 때의 처리 방식

예시 코드:

```dart
Stack(
  alignment: Alignment.center,
  children: <Widget>[
    Container(
      width: 300,
      height: 300,
      color: Colors.red,
    ),
    Positioned(
      top: 80,
      left: 80,
      child: Container(
        width: 150,
        height: 150,
        color: Colors.green,
      ),
    ),
    Text('Hello, World!'),
  ],
)
```

Stack은 복잡한 UI 레이아웃을 만들 때 유용하며, 특히 위젯들을 겹치게 배치해야 할 때 자주 사용된다.