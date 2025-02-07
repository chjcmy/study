														₩₩₩₩₩₩### Riverpod 소개
- **정의**: Flutter의 **반응형 상태 관리 및 의존성 주입 프레임워크**
- **목적**: 
  - 상태 관리 복잡성 해결
  - 컴파일 타임 안정성 보장
  - 테스트 용이성 향상
- **특징**: `Provider`의 개선판, 컨텍스트 독립적, 자동 캐싱, 의존성 관리

### 핵심 개념
1. **Provider**:
   - 상태의 단일 진실 공급원(SSOT)
   - `final counterProvider = StateProvider<int>((ref) => 0)`
2. **Consumer**:
   - `ConsumerWidget`/`ConsumerStatefulWidget` 사용
   - `ref.watch()`로 상태 관찰
3. **Ref 객체**:
   - 위젯과 Provider 간의 상호작용 매개체
   - `watch`, `read`, `listen` 메서드 제공

### 기본 사용법
```dart
// Provider 정의
final counterProvider = StateProvider<int>((ref) => 0);

// ConsumerWidget 사용
class CounterPage extends ConsumerWidget {
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final counter = ref.watch(counterProvider);
    return Text('Count: $counter');
  }
}
```

---

## 주요 Provider 유형
| 유형 | 설명 | 사용 예시 |
|------|------|-----------|
| `Provider` | 정적 데이터 제공 | 설정값, 상수 |
| `StateProvider` | 단순 상태 관리 | 카운터, 토글 |
| `StateNotifierProvider` | 복잡한 비즈니스 로직 | 사용자 인증 |
| `FutureProvider` | 비동기 데이터 처리 | API 호출 결과 |
| `StreamProvider` | 실시간 스트림 데이터 | 실시간 채팅 |

---

## 고급 패턴
4. **AutoDispose**:
   ```dart
   final userProvider = StateNotifierProvider.autoDispose<UserNotifier, User>((ref) {
     return UserNotifier();
   });
   ```
5. **Family**:
   ```dart
   final todoProvider = FutureProvider.family<Todo, int>((ref, id) async {
     return fetchTodo(id);
   });
   ```
6. **상태 결합**:
   ```dart
   final filteredListProvider = Provider<List<Item>>((ref) {
     final filter = ref.watch(filterProvider);
     return ref.watch(itemListProvider).where(filter).toList();
   });
   ```

---

