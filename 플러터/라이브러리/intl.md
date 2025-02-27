**1. ARB 파일의 역할**

• **ARB 파일**은 “Application Resource Bundle”의 약자로, 각 언어별로 번역 문자열을 JSON 형식으로 정의합니다.

• 예를 들어, 아래 **intl_en.arb** 파일에서는 영어 번역 문자열이 정의되어 있다:

```
{
    "@@locale": "en",
    "shopping": "Shopping",
    "searchProduct": "Search product",
    "noProduct": "There is no product."
}
```

이 파일은 lib/util/lang/l10n 폴더에 저장됩니다.

  

**2. flutter_intl 설정**

• **pubspec.yaml**에 flutter_intl 설정을 추가하여, ARB 파일이 위치한 폴더와 생성된 Dart 번역 파일의 경로를 지정합니다.

• 예시 설정:

```
flutter_intl:
  enabled: true
  arb_dir: lib/util/lang/l10n
  output_dir: lib/util/lang/generated
```

  

  

**3. ARB 파일 → 메시지 생성**

• ARB 파일(예: intl_en.arb, intl_ko.arb)에 정의된 번역 키와 문자열을 기반으로, 플러터 intl 도구가 자동으로 Dart 코드를 생성합니다.

• 이 도구는 보통 아래와 같은 명령어로 실행합니다:

```
flutter pub run intl_utils:generate
```

  

• 생성된 파일 중 하나가 **messages_all.dart**로, 이 파일은 모든 로케일에 대한 번역 메시지를 모아두고,

각 언어에 맞는 메시지를 로드하는 initializeMessages(localeName) 함수와 메시지 조회를 위한 delegate(S.delegate)를 포함한다.

  

**4. 생성된 코드의 역할**

• **initializeMessages(localeName)**:

특정 로케일의 ARB 파일에서 번역 메시지를 로드한다.

• **S.delegate**:

MaterialApp의 localizationsDelegates에 설정되어, Flutter 프레임워크가 올바른 로케일의 메시지를 가져오도록 한다.

• **S 클래스**:

ARB 파일에 정의된 각 번역 키에 대해 getter 함수(예: S.current.shopping)를 자동으로 생성하여, 앱 전반에서 번역된 문자열에 쉽게 접근할 수 있게 해준다.

  

**5. 실제 사용 예**

• 예를 들어, MaterialApp에서 아래와 같이 localizationsDelegates와 supportedLocales를 설정합니다:

```
MaterialApp(
  localizationsDelegates: const [
    S.delegate,
    GlobalMaterialLocalizations.delegate,
    GlobalWidgetsLocalizations.delegate,
    GlobalCupertinoLocalizations.delegate,
  ],
  supportedLocales: S.delegate.supportedLocales,
  // ...
)
```

  

• 앱 내에서는 S.current.shopping, S.current.searchProduct 등으로 번역 문자열에 접근할 수 있다.

이와 같이 ARB 파일에 번역 내용을 저장하면, 플러터 intl 도구가 자동으로 messages_all.dart를 비롯한 관련 코드를 생성해 주어, 앱에서 간편하게 국제화 기능을 사용할 수 있습니다. 이러한 과정과 내용을 Obsidian에 정리하면, 나중에 프로젝트를 유지보수하거나 새로운 언어를 추가할 때 큰 도움이 된다.