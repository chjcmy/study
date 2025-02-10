# BuildContext의 이해

BuildContext는 Flutter에서 위젯 트리 내의 위젯의 위치를 나타내는 핵심 개념이다.

## 주요 특징

1. **위치 정보**: 위젯 트리 내에서 현재 위젯의 위치를 나타낸다.

2. **상위 위젯 접근**: 상위 위젯의 정보에 접근할 수 있게 해준다.

3. **의존성 제공**: InheritedWidget을 통해 하위 위젯에 데이터를 제공한다.

4. **테마 및 미디어 쿼리**: 앱의 테마와 디바이스 정보에 접근할 수 있게 한다.

## 주요 사용 사례

1. **Theme 접근**:
   ```dart
   Theme.of(context).textTheme.headline1
   ```

2. **Navigator 사용**:
   ```dart
   Navigator.of(context).push(MaterialPageRoute(...))
   ```

3. **Scaffold 접근**:
   ```dart
   Scaffold.of(context).showSnackBar(SnackBar(...))
   ```

4. **MediaQuery 사용**:
   ```dart
   MediaQuery.of(context).size.width
   ```

## 주의사항

- build 메서드 외부에서 BuildContext를 사용할 때는 주의가 필요하다.
- 비동기 작업에서 BuildContext를 사용할 때는 위젯이 여전히 트리에 있는지 확인해야 한다.

BuildContext의 올바른 이해와 사용은 Flutter 앱 개발에서 효과적인 위젯 관리와 데이터 흐름 제어에 중요하다.