Flutter에서 위젯(Widget)은 UI를 구성하는 기본 단위이다. 모든 UI 요소는 위젯으로 표현되며, 위젯은 크게 두 가지 종류로 나뉜다: Stateless 위젯과 Stateful 위젯.

## 위젯의 개념

위젯은 화면에 표시되는 모든 요소를 나타낸다. 버튼, 텍스트, 이미지, 레이아웃 등 모든 것이 위젯이다. Flutter의 UI는 이러한 위젯들의 조합으로 구성된다.

## Stateless 위젯

Stateless 위젯은 상태를 가지지 않는 정적인 위젯이다.

특징:
- 한 번 생성되면 변경되지 않는다.
- 속성(properties)은 final로 선언된다.
- build 메서드를 통해 UI를 구성한다.

예시:
```dart
class MyStatelessWidget extends StatelessWidget {
  final String text;

  const MyStatelessWidget({Key? key, required this.text}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      child: Text(text),
    );
  }
}
```

## Stateful 위젯

Stateful 위젯은 상태를 가지며, 상태에 따라 UI가 변경될 수 있는 동적인 위젯이다.

특징:
- 상태를 가지고 있어 데이터가 변경될 때 UI를 다시 그릴 수 있다.
- createState() 메서드를 통해 State 객체를 생성한다.
- State 클래스의 setState() 메서드를 호출하여 상태 변경을 알린다.

예시:
```dart
class MyStatefulWidget extends StatefulWidget {
  const MyStatefulWidget({Key? key}) : super(key: key);

  @override
  _MyStatefulWidgetState createState() => _MyStatefulWidgetState();
}

class _MyStatefulWidgetState extends State<MyStatefulWidget> {
  int _counter = 0;

  void _incrementCounter() {
    setState(() {
      _counter++;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Text('Count: $_counter'),
        ElevatedButton(
          onPressed: _incrementCounter,
          child: Text('Increment'),
        ),
      ],
    );
  }
}
```

## 위젯 선택 기준

- Stateless 위젯: 외부에서 전달받은 데이터만을 표시하고, 내부적으로 상태 변경이 필요 없는 경우 사용한다.
- Stateful 위젯: 사용자 상호작용이나 데이터 변경에 따라 UI를 동적으로 업데이트해야 하는 경우 사용한다.

위젯의 적절한 선택과 사용은 Flutter 앱의 성능과 유지보수성에 큰 영향을 미친다. 상황에 맞는 위젯을 선택하여 효율적인 UI를 구성하는 것이 중요하다.