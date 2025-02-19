**📌 Flutter AlertDialog 정리 (Obsidian용)**

**🛠️ AlertDialog 개요**

Flutter의 **AlertDialog**는 **사용자에게 중요한 정보를 제공**하거나 **결정(확인/취소 등)**을 요구할 때 사용되는 모달 대화 상자입니다.

• **다이얼로그(Dialog)**: 화면 위에 뜨는 팝업 창

• **모달 방식**: 사용자가 닫기 전까지 다른 UI와 상호작용 불가

• **버튼**을 추가하여 사용자의 선택을 받을 수 있음

**🎯 주요 특성**

|**속성**|**설명**|
|---|---|
|title|다이얼로그의 제목을 나타냄 (보통 Text 위젯 사용)|
|content|다이얼로그의 본문 내용을 표시 (보통 Text 또는 Widget)|
|actions|하단 버튼 목록 (예: **확인, 취소**)|
|shape|다이얼로그의 테두리 모양 설정 (ex. RoundedRectangleBorder)|
|backgroundColor|다이얼로그 배경색|
|elevation|그림자 깊이 (기본값: 24)|
|titleTextStyle|제목 스타일 (TextStyle)|
|contentTextStyle|본문 스타일 (TextStyle)|

**📝 기본 사용법**

```dart
showDialog(
  context: context,
  builder: (context) {
    return AlertDialog(
      title: Text("경고"),
      content: Text("이 작업을 실행하시겠습니까?"),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: Text("취소"),
        ),
        TextButton(
          onPressed: () {
            // 실행 로직
            Navigator.pop(context);
          },
          child: Text("확인"),
        ),
      ],
    );
  },
);
```

**🎨 커스텀 AlertDialog 예제**

```dart
showDialog(
  context: context,
  builder: (context) {
    return AlertDialog(
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
      ),
      backgroundColor: Colors.black87,
      title: Text(
        "알림",
        style: TextStyle(color: Colors.white),
      ),
      content: Text(
        "이 메시지는 커스텀 다이얼로그입니다.",
        style: TextStyle(color: Colors.white70),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: Text("닫기", style: TextStyle(color: Colors.blue)),
        ),
      ],
    );
  },
);
```

• **둥근 모서리 적용 (shape)**

• **배경색 변경 (backgroundColor)**

• **텍스트 스타일 변경 (titleTextStyle, contentTextStyle)**

**🚀 ConsumerWidget에서 AlertDialog 사용하기**

  

Riverpod을 사용하는 **ConsumerWidget**에서 AlertDialog를 사용하려면 **ref를 builder 내부에서 호출**해야 함.

```dart
class BaseDialog extends ConsumerWidget {
  const BaseDialog({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return AlertDialog(
      title: Text("BaseDialog"),
      content: Text("이 다이얼로그는 ConsumerWidget 내부에서 사용됩니다."),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: Text("확인"),
        ),
      ],
    );
  }
}
```

**⚡ AlertDialog vs 다른 다이얼로그 비교**

|**다이얼로그 종류**|**설명**|
|---|---|
|**AlertDialog**|기본적인 경고/확인용 다이얼로그|
|**SimpleDialog**|옵션 선택을 제공하는 간단한 다이얼로그|
|**Dialog**|AlertDialog보다 더 자유롭게 커스텀 가능한 일반 다이얼로그|
|**BottomSheet**|화면 하단에서 올라오는 다이얼로그 스타일|

**💡 팁**

• showDialog() 사용 시 **barrierDismissible: false** 설정하면, **다이얼로그 외부를 눌러도 닫히지 않도록 설정** 가능

• 버튼이 여러 개일 경우 actionsAlignment: MainAxisAlignment.end 속성을 추가하면 **버튼 정렬 가능**

• showDialog() 내부에서 StatefulWidget을 사용하면, 다이얼로그 내에서도 **상태 관리** 가능

**✅ 정리**

• AlertDialog는 Flutter에서 **경고창이나 사용자 입력 확인용** 다이얼로그로 많이 사용됨.

• **title**, **content**, **actions**을 조합하여 다양한 형태로 구성 가능.

• **Riverpod과 함께 사용할 경우**, ConsumerWidget 또는 builder 내부에서 ref를 호출해야 함.

• **커스텀 스타일**을 활용하면 더욱 사용자 친화적인 UI 구현 가능.

**📌 Obsidian 태그 추천**

#flutter #dialog #ui #alertdialog #riverpod