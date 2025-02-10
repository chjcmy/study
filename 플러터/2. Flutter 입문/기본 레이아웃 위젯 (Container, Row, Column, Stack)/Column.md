Column은 Flutter에서 자식 위젯들을 수직으로 배치하는 레이아웃 위젯이다. 주요 특징은 다음과 같다:

1. 여러 자식 위젯을 수직 방향으로 배열한다.

2. mainAxisAlignment로 주 축(수직) 정렬을 조정할 수 있다.

3. crossAxisAlignment로 교차 축(수평) 정렬을 조정할 수 있다.

4. children 속성에 위젯 리스트를 제공하여 자식 위젯들을 지정한다.

Column의 주요 속성들:

- children: 배치할 자식 위젯들의 리스트
- mainAxisAlignment: 주 축 방향으로의 정렬 방식
- crossAxisAlignment: 교차 축 방향으로의 정렬 방식
- mainAxisSize: Column의 주 축 방향 크기 (max 또는 min)
- verticalDirection: 수직 방향 (위에서 아래로 또는 아래에서 위로)

예시 코드:

```dart
Column(
  mainAxisAlignment: MainAxisAlignment.center,
  crossAxisAlignment: CrossAxisAlignment.start,
  children: <Widget>[
    Text('First'),
    Text('Second'),
    Text('Third'),
  ],
)
```

Column은 수직 방향으로 위젯을 배치해야 할 때 사용되며, 폼 레이아웃, 리스트 아이템, 카드 내용 등을 구성하는 데 자주 활용된다.