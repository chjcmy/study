Flutter에서 기본 레이아웃 위젯은 UI를 구성하는 핵심 요소이다. 주요 레이아웃 위젯은 다음과 같다:

## [[플러터/2. Flutter 입문/기본 레이아웃 위젯 (Container, Row, Column, Stack)/Container|Container]]

Container는 단일 자식 위젯을 포함할 수 있는 박스 모델 위젯이다.

주요 특징:
- 패딩, 마진, 테두리 등을 설정할 수 있다.
- 배경색, 크기 등을 지정할 수 있다.

예시:
```dart
Container(
  padding: EdgeInsets.all(8.0),
  margin: EdgeInsets.symmetric(vertical: 10.0),
  decoration: BoxDecoration(
    color: Colors.blue,
    borderRadius: BorderRadius.circular(5.0),
  ),
  child: Text('Hello'),
)
```

## [[플러터/2. Flutter 입문/기본 레이아웃 위젯 (Container, Row, Column, Stack)/Row|Row]]

Row는 자식 위젯들을 수평으로 배치하는 위젯이다.

주요 특징:
- mainAxisAlignment로 주 축(수평) 정렬을 조정할 수 있다.
- crossAxisAlignment로 교차 축(수직) 정렬을 조정할 수 있다.

예시:
```dart
Row(
  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
  children: <Widget>[
    Icon(Icons.star),
    Icon(Icons.star),
    Icon(Icons.star),
  ],
)
```

## [[플러터/2. Flutter 입문/기본 레이아웃 위젯 (Container, Row, Column, Stack)/Column|Column]]

Column은 자식 위젯들을 수직으로 배치하는 위젯이다.

주요 특징:
- mainAxisAlignment로 주 축(수직) 정렬을 조정할 수 있다.
- crossAxisAlignment로 교차 축(수평) 정렬을 조정할 수 있다.

예시:
```dart
Column(
  mainAxisAlignment: MainAxisAlignment.center,
  children: <Widget>[
    Text('First'),
    Text('Second'),
    Text('Third'),
  ],
)
```

## [[플러터/2. Flutter 입문/기본 레이아웃 위젯 (Container, Row, Column, Stack)/Stack|Stack]]

Stack은 자식 위젯들을 겹쳐서 배치할 수 있는 위젯이다.

주요 특징:
- 자식 위젯들을 z-축으로 쌓아올릴 수 있다.
- Positioned 위젯을 사용하여 자식 위젯의 위치를 정확히 지정할 수 있다.

예시:
```dart
Stack(
  children: <Widget>[
    Container(color: Colors.yellow, width: 300, height: 300),
    Positioned(
      top: 80,
      left: 80,
      child: Container(color: Colors.red, width: 150, height: 150),
    ),
  ],
)
```

이러한 기본 레이아웃 위젯들을 조합하여 복잡한 UI를 구성할 수 있다. 각 위젯의 특성을 이해하고 적절히 활용하는 것이 Flutter 앱 개발의 핵심인거 같다