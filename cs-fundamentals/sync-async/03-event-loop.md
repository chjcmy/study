# 이벤트 루프 (Event Loop)

---

## 동작 원리

```
FastAPI/uvicorn의 핵심 엔진 — 단일 스레드로 수만 동시 연결 처리

이벤트 루프 (단일 스레드)
┌──────────────────────────────────────────────────┐
│                                                  │
│  1. 이벤트 큐에서 실행 가능한 작업 꺼내기          │
│     ↓                                            │
│  2. 실행                                         │
│     ├── CPU 작업이면 → 즉시 완료                  │
│     └── I/O 작업이면 → OS에 맡기고 다음 작업으로  │
│     ↓                                            │
│  3. I/O 완료 알림 수신 → 해당 코루틴 재개          │
│     ↓                                            │
│  4. 반복                                         │
│                                                  │
└──────────────────────────────────────────────────┘

핵심: "I/O 대기 = 낭비" → 대기 시간에 다른 작업 처리!
```

---

## 동작 예시 상세

```
요청 3개가 거의 동시에 도착:

시간(ms) │ 이벤트 루프 동작
─────────┼──────────────────────────────────
0        │ 요청A 수신 → get_tenant() 시작
1        │   await db.query() → OS에 맡김 (코루틴 A 일시정지)
         │   큐에서 다음 꺼냄
2        │ 요청B 수신 → get_subscriptions() 시작
3        │   await httpx.get() → OS에 맡김 (코루틴 B 일시정지)
         │   큐에서 다음 꺼냄
4        │ 요청C 수신 → health_check() ← CPU만 필요, 즉시 완료
5        │   응답C 전송 ✅
         │   큐 확인... (대기)
50       │ DB 응답 도착! → 코루틴 A 재개
51       │   데이터 가공 → 응답A 전송 ✅
100      │ ARM API 응답 도착! → 코루틴 B 재개
101      │   데이터 가공 → 응답B 전송 ✅

→ 1스레드로 3개 요청을 "동시" 처리!
→ 각 I/O 대기 시간에 다른 요청 처리
```

---

## 내부 구조

```
asyncio 이벤트 루프 내부 컴포넌트:

┌──────────────────────────────────────────┐
│              이벤트 루프                   │
│                                          │
│  ┌──────────┐   Ready Queue              │
│  │ Selector │   ┌────┬────┬────┐         │
│  │ (epoll)  │   │코루│코루│코루│         │
│  │ I/O 감시 │   │틴A │틴B │틴C │         │
│  └────┬─────┘   └────┴────┴────┘         │
│       │                                  │
│       │ I/O 이벤트 발생                    │
│       │ → 해당 코루틴을 Ready Queue로 이동  │
│       │                                  │
│  ┌────▼─────┐                            │
│  │Scheduled │  Timer Queue               │
│  │ Tasks    │  asyncio.sleep(), 타임아웃  │
│  └──────────┘                            │
└──────────────────────────────────────────┘

한 사이클 (Tick):
  1. Ready Queue에서 실행 가능한 코루틴 꺼내서 실행
  2. Timer Queue 확인 (시간 도래한 것 Ready로 이동)
  3. Selector.select() → I/O 완료된 것 Ready로 이동
  4. 반복
```

---

## Node.js vs Python asyncio 비교

| 특성 | Node.js | Python asyncio |
|------|---------|---------------|
| 언어 | JavaScript | Python |
| 이벤트 루프 | libuv (C) | **uvloop** (libuv 바인딩) |
| 문법 | async/await, Promise | async/await, coroutine |
| 성능 | V8 엔진 (JIT) | CPython (인터프리터) |
| GIL | 없음 | 있음 (싱글 스레드) |
| CPU 바운드 | Worker Threads | asyncio.to_thread |

```
공통점: 싱글 스레드 + 이벤트 루프 + 논블로킹 I/O
차이점: Node는 기본 비동기, Python은 명시적 async/await

uvloop (FastAPI 기본):
  libuv를 Python에 바인딩
  기본 asyncio보다 2~4배 빠름
  Cython으로 작성
```

---

## 이벤트 루프 주의사항

```python
# ⚠️ 이벤트 루프 블로킹 = 전체 서버 멈춤!

# ❌ CPU 바운드 작업을 이벤트 루프에서 실행
@app.get("/bad")
async def bad():
    result = heavy_calculation()  # ← 10초 동안 루프 블로킹!
    return result
    # → 10초 동안 다른 모든 요청이 멈춤!

# ✅ 해결 1: 스레드 풀에 위임
@app.get("/good")
async def good():
    result = await asyncio.to_thread(heavy_calculation)
    return result

# ✅ 해결 2: def로 선언 (FastAPI가 자동으로 스레드 풀 사용)
@app.get("/good2")
def good2():  # async 없음 → 자동으로 스레드 풀
    result = heavy_calculation()
    return result
```

---

## 면접 핵심 포인트

```
Q: 이벤트 루프의 동작 원리?
A: 단일 스레드가 무한 루프를 돌며:
   1) Ready Queue에서 실행 가능한 코루틴 실행
   2) I/O 완료 이벤트 수신 (epoll) → 해당 코루틴 재개
   → I/O 대기 시간에 다른 코루틴 처리하여 높은 처리량 달성

Q: 이벤트 루프가 블로킹되면?
A: 모든 동시 요청이 멈춤! 
   → CPU 바운드는 스레드 풀(to_thread)로 분리
   → 동기 I/O(requests) 대신 비동기 I/O(httpx) 사용

Q: 싱글 스레드인데 어떻게 동시 처리?
A: "동시 처리"가 아니라 "동시성". 한 번에 하나만 실행하지만,
   I/O 대기 중 다른 작업으로 전환하여 "동시에 하는 것처럼" 보임.
   진짜 병렬은 멀티 워커(multi-process)로 달성.
```
