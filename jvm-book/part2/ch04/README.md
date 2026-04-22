# 4장 성능 모니터링 도구 요약

> **"JVM 밑바닥까지 파헤치기"** (深入理解Java虚拟机 3판, 저우즈밍)

---

## CLI 도구 치트시트

| 도구 | 목적 | 핵심 사용법 | 실무 시나리오 |
|------|------|-----------|-------------|
| **jps** | JVM 프로세스 목록 | `jps -lv` | 실행 중인 자바 앱 PID 확인 |
| **jstat** | GC/클래스/컴파일 통계 | `jstat -gcutil <pid> 1000 10` | GC 빈도/소요 시간 모니터링 |
| **jinfo** | JVM 설정 조회/변경 | `jinfo -flag MaxHeapSize <pid>` | 런타임 중 플래그 확인 |
| **jmap** | 힙 덤프/히스토그램 | `jmap -dump:live,format=b,file=heap.hprof <pid>` | OOM 원인 분석 |
| **jhat** | 힙 덤프 분석 서버 | `jhat heap.hprof` (→ 브라우저) | 간단한 힙 분석 (MAT 권장) |
| **jstack** | 스레드 덤프 | `jstack -l <pid>` | 데드락, 스레드 블로킹 진단 |

---

## 실습: log-friends 에이전트 모니터링

```bash
# 1. 실행 중인 log-friends 앱의 PID 확인
jps -lv | grep examples

# 2. GC 통계를 1초 간격으로 모니터링
jstat -gcutil <pid> 1000
#   S0     S1     E      O      M     CCS    YGC     YGCT    FGC    FGCT     GCT
#   0.00  45.23  67.12  12.34  95.67  92.45    15    0.123     1    0.045   0.168

# 각 열 의미:
# S0/S1:  Survivor 0/1 사용률(%)
# E:      Eden 사용률(%)
# O:      Old Gen 사용률(%)
# M:      Metaspace 사용률(%)
# YGC:    Young GC 횟수
# YGCT:   Young GC 누적 시간(초)
# FGC:    Full GC 횟수
# FGCT:   Full GC 누적 시간(초)

# 3. Metaspace 사용량 확인 (ByteBuddy 클래스 생성 모니터링)
jstat -gcmetacapacity <pid> 1000

# 4. 힙 히스토그램 (상위 20개 클래스)
jmap -histo:live <pid> | head -25

# 5. 스레드 덤프 (log-friends-batch-flush 스레드 상태 확인)
jstack <pid> | grep -A 20 "log-friends-batch-flush"

# 6. 힙 덤프 후 MAT로 분석
jmap -dump:live,format=b,file=logfriends-heap.hprof <pid>
```

---

## GUI 도구 요약

| 도구 | 특징 | 용도 |
|------|------|------|
| **JHSDB** | JDK 내장, SA(Serviceability Agent) 기반 | 코어 덤프 분석, 메모리 저수준 분석 |
| **JConsole** | JDK 내장, JMX 기반 | 실시간 메모리/스레드/MBean 모니터링 |
| **VisualVM** | 플러그인 확장 가능 | 프로파일링, 힙 분석, 스레드 분석 (가장 범용적) |
| **JMC (Java Mission Control)** | JFR 기반, 저오버헤드 | 프로덕션 환경 프로파일링 (상시 모니터링에 적합) |

---

## 핫스팟 플러그인

```
HSDIS:    JIT 컴파일러가 생성한 네이티브 코드를 어셈블리로 디스어셈블
          → -XX:+UnlockDiagnosticVMOptions -XX:+PrintAssembly

JITWatch: JIT 컴파일 로그를 시각화
          → -XX:+UnlockDiagnosticVMOptions -XX:+LogCompilation
          → JIT 인라인 여부, 컴파일 큐 확인
```

---

## 학습 완료 체크리스트

- [ ] `jstat -gcutil`의 각 열(S0, S1, E, O, M, YGC, FGC)의 의미를 해석할 수 있다
- [ ] `jmap -dump`로 힙 덤프를 생성하고 MAT로 분석할 수 있다
- [ ] `jstack`으로 데드락과 스레드 블로킹을 진단할 수 있다
