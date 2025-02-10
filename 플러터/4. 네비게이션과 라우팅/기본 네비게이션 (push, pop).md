Flutter에서 기본 네비게이션은 주로 Navigator 클래스를 사용하여 구현한다. 주요 개념인 push와 pop은 다음과 같이 작동한다:

1. Push (화면 추가):
   - 새로운 화면을 현재 화면 위에 추가한다.
   - Navigator.push() 메서드를 사용한다.

   예시:
   ```dart
   Navigator.push(
     context,
     MaterialPageRoute(builder: (context) => SecondScreen()),
   );
   ```

2. Pop (화면 제거):
   - 현재 화면을 제거하고 이전 화면으로 돌아간다.
   - Navigator.pop() 메서드를 사용한다.

   예시:
   ```dart
   Navigator.pop(context);
   ```

3. 데이터 전달:
   - Push: 새 화면으로 데이터를 전달할 수 있다.
   - Pop: 이전 화면으로 결과를 반환할 수 있다.

4. 명명된 라우트:
   - 미리 정의된 라우트 이름을 사용하여 네비게이션할 수 있다.
   - Navigator.pushNamed() 메서드를 사용한다.

이러한 기본 네비게이션 기능을 사용하여 다중 화면 앱을 쉽게 구현할 수 있다.