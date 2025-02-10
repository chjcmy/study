Container는 Flutter에서 가장 기본적이고 자주 사용되는 레이아웃 위젯 중 하나이다. 주요 특징은 다음과 같다:

1. 단일 자식 위젯을 포함할 수 있는 박스 모델 위젯이다.

2. 패딩, 마진, 테두리 등을 설정할 수 있다.

3. 배경색, 크기 등을 지정할 수 있다.

4. decoration 속성을 통해 다양한 스타일링이 가능하다.

Container의 주요 속성들:

- child: 포함할 자식 위젯
- width, height: 컨테이너의 크기 지정
- padding: 내부 여백
- margin: 외부 여백
- decoration: 배경색, 테두리, 그림자 등 스타일링
- alignment: 자식 위젯의 정렬 방식
- color: 배경색 (decoration과 함께 사용 불가)

예시 코드:

```dart
Container(
  width: 200,
  height: 100,
  padding: EdgeInsets.all(8.0),
  margin: EdgeInsets.symmetric(vertical: 10.0),
  decoration: BoxDecoration(
    color: Colors.blue,
    borderRadius: BorderRadius.circular(10.0),
  ),
  child: Text('Hello'),
)
```

Container는 UI 구성의 기본 블록으로 사용되며, 다른 레이아웃 위젯들과 조합하여 복잡한 인터페이스를 구축하는 데 활용된다.
