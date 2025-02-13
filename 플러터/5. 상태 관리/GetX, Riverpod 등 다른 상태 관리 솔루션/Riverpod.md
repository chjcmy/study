아래는 **Obsidian 형식**으로 ConsumerWidget을 추가하여 정리한 내용입니다.

**🏗 Riverpod in Flutter**

  

📌 **Riverpod**는 Flutter에서 상태 관리를 위해 사용되는 라이브러리이다.

Provider의 개선된 버전으로, 더 **안전하고 유연한 상태 관리**를 제공한다.

**🟢 Riverpod의 핵심 개념**

1. **Provider** → 상태나 값을 제공하는 객체

2. **ConsumerWidget** → Provider의 값을 사용하는 위젯

3. **ref** → Provider에 접근하기 위한 객체

**⚙ Riverpod 설정 방법**

  

**1️⃣ 의존성 추가**

  

pubspec.yaml 파일에 flutter_riverpod 패키지를 추가한다.

```
dependencies:
  flutter:
    sdk: flutter
  flutter_riverpod: ^2.3.6
```

**2️⃣ 패키지 설치**

```
flutter pub get
```

**🏗 Provider 생성 및 사용**

  

**📌 예제: 카운터 앱**

```
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

// ✅ 상태를 관리할 Provider 정의
final counterProvider = StateProvider((ref) => 0);

void main() {
  runApp(
    // ✅ Riverpod을 앱 전체에 적용
    ProviderScope(
      child: MyApp(),
    ),
  );
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: HomePage(),
    );
  }
}

// ✅ ConsumerWidget 사용
class HomePage extends ConsumerWidget {
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    // ✅ Provider의 상태를 구독
    final count = ref.watch(counterProvider);

    return Scaffold(
      appBar: AppBar(title: Text('Riverpod Counter')),
      body: Center(
        child: Text(
          'Count: $count',
          style: TextStyle(fontSize: 24),
        ),
      ),
      floatingActionButton: FloatingActionButton(
        // ✅ Provider의 상태 변경
        onPressed: () => ref.read(counterProvider.notifier).state++,
        child: Icon(Icons.add),
      ),
    );
  }
}
```

**🟡 Riverpod의 주요 특징**

4. **컴파일 타임 안전성** → 존재하지 않는 Provider에 접근 시 컴파일 에러 발생

5. **Provider 재정의 가능** → 테스트 및 개발 환경에서 손쉽게 재정의 가능

6. **코드 생성 불필요** → 별도의 코드 생성 없이 사용 가능

  

✅ Riverpod을 사용하면 상태 관리가 간단해지고, 코드의 가독성과 유지보수성이 향상된다.

**🔹 ConsumerWidget**

  

📌 ConsumerWidget은 **Riverpod의 Provider를 구독하는 Flutter 위젯**이다.

즉, **Provider 상태가 변경될 때 자동으로 UI를 업데이트**한다.

  

**🟢 ConsumerWidget의 특징**

• StatelessWidget과 동일하지만, **ref.watch()를 통해 Provider 상태를 직접 구독 가능**

• BuildContext 없이도 riverpod 상태를 쉽게 가져올 수 있음

• ref.read()를 통해 Provider의 값을 업데이트 가능

**📌 ConsumerWidget 기본 사용 예제**

```
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

// ✅ Provider 선언
final counterProvider = StateProvider<int>((ref) => 0);

class CounterScreen extends ConsumerWidget {
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final count = ref.watch(counterProvider); // ✅ Provider 구독

    return Scaffold(
      appBar: AppBar(title: Text('ConsumerWidget Example')),
      body: Center(
        child: Text(
          'Count: $count',
          style: TextStyle(fontSize: 24),
        ),
      ),
      floatingActionButton: FloatingActionButton(
        // ✅ 상태 변경 (값 증가)
        onPressed: () => ref.read(counterProvider.notifier).state++,
        child: Icon(Icons.add),
      ),
    );
  }
}
```

✔ ref.watch(provider) → **Provider의 값을 구독하여 UI를 자동으로 업데이트**

✔ ref.read(provider.notifier).state = newValue; → **Provider의 상태를 변경**

**🔹 ConsumerWidget vs Consumer**

|**기능**|**ConsumerWidget**|**Consumer**|
|---|---|---|
|**사용 목적**|전체 위젯에서 ref.watch() 사용|특정 위젯에서만 ref.watch() 사용|
|**위젯 타입**|StatelessWidget을 대체|기존 StatelessWidget에서도 사용 가능|
|**최적화**|전체 위젯이 ref.watch()를 사용할 수 있음|특정 위젯만 ref.watch() 적용 가능|

**✅ Consumer 사용 예제**

```
class CounterScreen extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Consumer(
      builder: (context, ref, child) {
        final count = ref.watch(counterProvider);
        return Text("Count: $count");
      },
    );
  }
}
```

✔ **Consumer를 사용하면 특정 위젯에서만 Provider 상태를 구독할 수 있어 최적화 가능!**

**🎯 결론**  

✔ **🔥 Riverpod은 Provider보다 더 안전하고 최적화된 상태 관리 방법을 제공한다.**

✔ **🎯 ConsumerWidget을 사용하면 ref.watch()를 활용하여 쉽게 Provider 상태를 구독할 수 있다.**

✔ **🚀 Consumer를 사용하면 특정 위젯에서만 ref.watch()를 적용하여 성능 최적화 가능!**

  

💡 **즉, Riverpod을 사용하면 Flutter 상태 관리를 더 쉽게 할 수 있으며, ConsumerWidget을 활용하면 불필요한 BuildContext 드릴링 없이 상태를 구독할 수 있다!** 🚀
