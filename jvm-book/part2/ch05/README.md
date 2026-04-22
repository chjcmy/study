# 5장 고성능 메모리 할당 — 최적화 사례 요약

> **"JVM 밑바닥까지 파헤치기"** (深入理解Java虚拟机 3판, 저우즈밍)

---

## 사례 1: 대용량 메모리 — 힙을 키운다고 무조건 좋은가?

```
문제: 64GB 서버에 -Xmx48g 설정 → Full GC 시 수십 초 STW
원인: Parallel Old GC가 48GB 힙을 컴팩트 하려면 오래 걸림

해결 방안:
  A) 여러 JVM 인스턴스로 분산 (각 4~8GB)
  B) ZGC/Shenandoah 사용 (대용량 힙에서도 STW < 10ms)
  C) G1 사용 시 -XX:MaxGCPauseMillis=200 설정

log-friends 적용:
  → Agent가 삽입된 앱이 대용량 힙이면 ZGC 권장
  → Agent 자체의 메모리 풋프린트는 작지만 (큐 10K + Kafka 버퍼 32MB)
     GC가 길어지면 flush() 스레드도 STW에 걸려 이벤트 전송 지연
```

---

## 사례 2: 클러스터 간 동기화 — 과도한 Full GC

```
문제: 분산 캐시 동기화 시 대량 객체 역직렬화 → Old Gen 급증 → Full GC 반복
원인: 캐시 데이터가 장기 생존 → Minor GC를 넘어 Old로 승격

해결: 캐시 크기 제한, TTL 설정, off-heap 캐시(EHCache, Caffeine)

log-friends 적용:
  → BatchTransporter의 flush()가 실패하면 buffer의 이벤트를 큐에 재삽입
  → 반복 실패 시 Old Gen에 AgentEvent 객체가 쌓일 수 있음
  → Kafka 연결 장애 시 dropCount를 모니터링하여 큐 폭주 방지
```

---

## 사례 3: 외부 명령어 fork — Runtime.exec()의 함정

```
문제: Runtime.exec("curl ...") 호출 시 JVM 프로세스를 fork
      → 부모 프로세스 메모리를 복사(Copy-on-Write) → 메모리 2배 소비

해결: HttpClient 사용, ProcessBuilder 최소화

log-friends 적용:
  → log-friends SDK는 외부 프로세스 fork 없음 (Kafka 클라이언트가 소켓 직접 관리)
  → 좋은 설계 사례: 네트워크 통신을 Java 라이브러리 레벨에서 해결
```

---

## 사례 4: 안전 지점(Safepoint) 지연

```
문제: 루프 안에서 int 카운터 사용 → JIT가 "counted loop"로 인식
      → 루프 백엣지에 안전 지점 미삽입 → GC STW가 루프 종료까지 대기

해결: 루프 변수를 long으로 변경 (uncounted loop → 백엣지에 안전 지점 삽입)
      또는 -XX:+UseCountedLoopSafepoints (JDK 14+ 기본)

log-friends 적용:
  → drainTo()는 내부적으로 LinkedBlockingQueue의 lock + 순회
  → @Synchronized + drainTo(buffer, batchSize)는 100건으로 제한 → 안전 지점 문제 낮음
  → 그러나 사용자 앱의 @Service 메서드에서 긴 counted loop 존재 시
    MethodTraceInterceptor의 타이밍이 부정확해질 수 있음
```

---

## 이클립스 구동 시간 줄이기 (5.3 실전)

```
최적화 접근법:
  1. GC 로그 수집: -Xlog:gc*:file=gc.log
  2. 병목 식별: 클래스 로딩 시간, GC 시간, JIT 컴파일 시간
  3. GC 튜닝: G1 사용, Young Gen 비율 조정
  4. 클래스 데이터 공유 (CDS): -XX:+UseAppCDS → 클래스 로딩 시간 단축
  5. AOT 컴파일: GraalVM native-image (JDK 21 CRaC 도 대안)

log-friends 적용:
  → Agent 설치 시점 최적화가 중요
  → LogFriendsInstaller가 EnvironmentPostProcessor에서 ByteBuddy를 설치
  → 5개 인터셉터 설치가 앱 기동 시간에 추가됨
  → CDS로 ByteBuddy 클래스를 미리 로딩하면 기동 시간 단축 가능
```

---

## 학습 완료 체크리스트

- [ ] 대용량 힙에서 Full GC 시간이 길어지는 이유와 해결 방안을 설명할 수 있다
- [ ] Runtime.exec()의 메모리 문제를 설명할 수 있다
- [ ] 안전 지점이 긴 루프에서 GC STW를 지연시킬 수 있는 원리를 설명할 수 있다

### log-friends 연결

- [ ] BatchTransporter의 큐가 GC에 미치는 영향을 분석할 수 있다
- [ ] ByteBuddy RETRANSFORMATION의 Metaspace 영향을 설명할 수 있다
- [ ] KafkaProducer 버퍼의 다이렉트 메모리 사용 가능성을 인식한다
- [ ] JDK 21에서 ZGC 적용 시 이점과 주의사항을 설명할 수 있다
