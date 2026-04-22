# 2부: 자동 메모리 관리

> **"JVM 밑바닥까지 파헤치기"** (深入理解Java虚拟机 3판, 저우즈밍)
> 범위: 2장 ~ 5장
> 예상 학습 시간: 15~18시간

---

## 개요

JVM의 자동 메모리 관리(Automatic Memory Management)는 개발자가 직접 메모리를 할당하고 해제하지 않아도 되도록 하는 핵심 기능이다. 이 부에서는 JVM이 내부적으로 메모리를 어떻게 구성하고, 사용하지 않는 객체를 어떻게 수거하며, 개발자가 이를 어떻게 모니터링하고 최적화할 수 있는지를 다룬다.

---

## 챕터 목록

| 장 | 제목 | 핵심 주제 | 예상 시간 |
|----|------|----------|----------|
| [2장](./ch02/README.md) | 자바 메모리 영역과 메모리 오버플로 | 런타임 데이터 영역, 객체 생성 과정, OOM 유형 | 3~4시간 |
| [3장](./ch03/README.md) | 가비지 컬렉터와 메모리 할당 전략 | GC 알고리즘, 클래식 GC 비교, ZGC/Shenandoah, 할당 전략 | 7~8시간 |
| [4장](./ch04/README.md) | 가상 머신 성능 모니터링/진단 도구 | jps/jstat/jmap/jstack, GUI 도구(VisualVM, JMC) | 2~3시간 |
| [5장](./ch05/README.md) | 고성능 메모리 할당 | 실전 최적화 사례, Safepoint 지연, Full GC 대응 | 3~4시간 |

---

## 2장: 자바 메모리 영역과 메모리 오버플로

JVM이 자바 프로그램을 실행하면 메모리를 여러 영역으로 나누어 관리한다.

- **런타임 데이터 영역**: PC 카운터, JVM 스택, 네이티브 메서드 스택, 힙, 메서드 영역(Metaspace), 다이렉트 메모리
- **객체 생성 5단계**: 클래스 로딩 체크 → 메모리 할당(포인터 범핑/프리 리스트, TLAB) → Zero-fill → 헤더 설정 → `<init>` 호출
- **OOM 유형**: 힙 OOM, 스택 오버플로, Metaspace OOM, 다이렉트 메모리 OOM

→ [2장 상세 보기](./ch02/README.md)

---

## 3장: 가비지 컬렉터와 메모리 할당 전략

JVM GC의 핵심 원리와 모든 컬렉터를 다룬다.

- **도달 가능성 분석**: GC Roots 7종, 참조 강도 4종(Strong/Soft/Weak/Phantom)
- **GC 알고리즘**: 마크-스윕, 마크-카피, 마크-컴팩트
- **핫스팟 구현**: OopMap, Safepoint, Safe Region, 카드 테이블, 삼색 마킹(SATB/증분 갱신)
- **클래식 GC**: Serial, ParNew, Parallel Scavenge, CMS, G1
- **저지연 GC**: Shenandoah, ZGC, Generational ZGC(JDK 21)
- **할당 전략**: Eden 우선 할당, 큰 객체 직접 Old 배치, 동적 나이 판정

→ [3장 상세 보기](./ch03/README.md)

---

## 4장: 가상 머신 성능 모니터링/진단 도구

JVM을 진단하는 CLI 도구와 GUI 도구를 실습 중심으로 정리한다.

- **CLI**: `jps`, `jstat`, `jinfo`, `jmap`, `jhat`, `jstack`
- **GUI**: JHSDB, JConsole, VisualVM, JMC(Java Mission Control)
- **핫스팟 플러그인**: HSDIS(어셈블리 출력), JITWatch(JIT 시각화)
- **실습**: log-friends 에이전트를 `jstat -gcutil`로 모니터링하는 시나리오 포함

→ [4장 상세 보기](./ch04/README.md)

---

## 5장: 고성능 메모리 할당

실전 사례를 통해 메모리 최적화 접근법을 익힌다.

- **사례 1**: 대용량 힙(-Xmx48g) + Parallel GC → Full GC 수십 초 STW → ZGC로 해결
- **사례 2**: 분산 캐시 역직렬화 → Old Gen 급증 → TTL/off-heap 캐시로 해결
- **사례 3**: `Runtime.exec()` fork → 메모리 2배 소비 → HttpClient 사용
- **사례 4**: `int` counted loop → Safepoint 미삽입 → GC STW 지연
- **기동 시간 최적화**: CDS, G1 튜닝, AOT 컴파일

→ [5장 상세 보기](./ch05/README.md)

---

## log-friends 프로젝트와의 연결

이 부의 내용은 log-friends SDK 구현에 직접 연결된다:

| 주제 | 관련 컴포넌트 | 연결 내용 |
|------|-------------|----------|
| GC 압력 | `BatchTransporter` | `LinkedBlockingQueue(10000)`의 Node 래핑 → 단명 객체 대량 발생 |
| Metaspace | `InstrumentationRegistry` | ByteBuddy RETRANSFORMATION → 변환 클래스 메타데이터 누적 |
| 다이렉트 메모리 | `KafkaProducer` | `RecordAccumulator` 32MB 버퍼, NIO DirectByteBuffer |
| GC Root | `BatchTransporter.companion` | `@Volatile` 정적 필드 = GC Root → 인스턴스 영속 생존 |
| ZGC 권장 | JVM 옵션 | JDK 21 Generational ZGC + `-XX:MaxMetaspaceSize=256m` |
| Safepoint | `flush()` 스레드 | `@Synchronized` + `drainTo()` → 안전 지점 내 STW 지연 가능성 |
