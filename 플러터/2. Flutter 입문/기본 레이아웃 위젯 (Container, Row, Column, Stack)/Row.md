Row는 Flutter에서 자식 위젯들을 수평으로 배치하는 레이아웃 위젯이다. 주요 특징은 다음과 같다:

1. 여러 자식 위젯을 수평 방향으로 배열한다.

2. mainAxisAlignment로 주 축(수평) 정렬을 조정할 수 있다.

3. crossAxisAlignment로 교차 축(수직) 정렬을 조정할 수 있다.

4. children 속성에 위젯 리스트를 제공하여 자식 위젯들을 지정한다.

Row의 주요 속성들:

- children: 배치할 자식 위젯들의 리스트
- mainAxisAlignment: 주 축 방향으로의 정렬 방식
- crossAxisAlignment: 교차 축 방향으로의 정렬 방식
- mainAxisSize: Row의 주 축 방향 크기 (max 또는 min)
- textDirection: 텍스트 방향 (좌에서 우로 또는 우에서 좌로)

예시 코드:

```dart
Row(
  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
  crossAxisAlignment: CrossAxisAlignment.center,
  children: <Widget>[
    Icon(Icons.star),
    Icon(Icons.star),
    Icon(Icons.star),
  ],
)
```

Row는 수평 방향으로 위젯을 배치해야 할 때 사용되며, 버튼 그룹, 아이콘 모음, 텍스트 필드와 라벨 등을 구성하는 데 자주 활용된다.