### 참고 본 개발자는 애플 제품을 사용하기 때문에 .zshrc 추가 했습니다

Flutter 개발 환경을 설정하기 위해서는 다음과 같은 절차를 따라야 한다:

1. Flutter SDK 설치
   - Flutter 공식 웹사이트에서 SDK를 다운로드한다.
   - 다운로드한 파일을 원하는 위치에 압축 해제한다. (예: ~/development/flutter)

2. 환경 변수 설정 (zsh 기준)
   - 터미널을 열고 zsh 설정 파일을 연다:
     ```
     vi ~/.zshrc
     ```
   - 파일 끝에 다음 라인을 추가한다:
     ```
     export PATH="$PATH:$HOME/development/flutter/bin"
     ```
   - 파일을 저장하고 닫는다.
   - 변경사항을 적용하기 위해 다음 명령어를 실행한다:
     ```
     source ~/.zshrc
     ```

3. Flutter 설치 확인
   - 터미널에서 다음 명령어를 실행하여 Flutter가 제대로 설치되었는지 확인한다:
     ```
     flutter --version
     ```

4. IDE 설치 및 설정
   - Android Studio 또는 Visual Studio Code를 설치한다.
   - IDE에서 Flutter 및 Dart 플러그인을 설치한다.

5. 추가 종속성 설치
   - Xcode (iOS 개발용)
   - Android SDK (Android 개발용)

6. Flutter Doctor 실행
   - 터미널에서 다음 명령어를 실행하여 모든 설정이 올바르게 되었는지 확인한다:
     ```
     flutter doctor
     ```

이러한 단계를 완료하면 Flutter 개발을 시작할 수 있는 환경이 준비된다.
