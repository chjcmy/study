# 코루틴 (Coroutine)

---

## 개념

```
일반 함수:  호출 → 실행 → 반환 (한 번에 끝, 중간에 멈출 수 없음)
코루틴:    호출 → 실행 → 일시정지 → 재개 → 일시정지 → ... → 반환

핵심: "실행 도중 멈추고(yield/await), 나중에 다시 이어서 실행"
```

### Python 코루틴의 진화

```
1. Generator (Python 2.2+):
   def gen():
       yield 1
       yield 2  # yield로 값 반환 + 일시정지

2. Generator-based coroutine (Python 3.3+):
   @asyncio.coroutine
   def fetch():
       data = yield from some_io()  # yield from으로 위임

3. Native coroutine (Python 3.5+) ⭐:
   async def fetch():
       data = await some_io()  # 현대적 문법
```

---

## 동작 원리

```python
async def get_tenant(tenant_id):
    print("1. 쿼리 시작")
    tenant = await db.get(tenant_id)    # ← 여기서 일시정지!
    #                                        이벤트 루프가 다른 코루틴 실행
    #                                        ...
    #                                        DB 응답 도착!
    print("2. 쿼리 완료")               # ← 여기서 재개!
    return tenant

# 코루틴 상태 머신:
# State 0: "1. 쿼리 시작" 실행, await → 일시정지 (State = 1)
# State 1: DB 응답 수신, "2. 쿼리 완료" 실행 → 반환
```

### 코루틴의 메모리 구조

```
스레드: 
  ┌──────────────┐
  │   스택        │  수 MB (고정 할당)
  │   ...        │  각 함수 호출 프레임
  │   ...        │
  └──────────────┘

코루틴:
  ┌──────────────┐
  │  프레임 객체   │  수 KB (필요한 만큼만)
  │  지역 변수    │  
  │  실행 위치    │  ← 어디까지 실행했는지
  └──────────────┘

→ 1만 코루틴 ≈ 수십 MB
→ 1만 스레드 ≈ 수~수십 GB!
```

---

## 코루틴 vs 스레드 vs 프로세스

| 구분 | 코루틴 | 스레드 | 프로세스 |
|------|--------|--------|---------|
| 전환 비용 | **~100ns** | ~1μs | ~10μs |
| 메모리 | **수 KB** | 수 MB | 수십 MB |
| 동시 개수 | **수만~수십만 개** | 수천 개 | 수십 개 |
| 동기화 | **불필요** (싱글 스레드) | 필요 (Lock) | 불필요 (격리) |
| 스케줄링 | 유저 스페이스 (협력적) | OS 커널 (선점적) | OS 커널 |
| 적합 | **I/O 바운드** | 혼합 | CPU 바운드 |
| 제어 | 프로그래머 (await) | OS (선점) | OS |

### 협력적 vs 선점적 스케줄링

```
선점적 (스레드/프로세스):
  OS가 강제로 실행 중단하고 다른 것에 CPU 할당
  → 프로그래머가 제어 불가
  → 타이머 인터럽트로 전환

협력적 (코루틴):
  코루틴이 자발적으로 양보 (await)해야 전환
  → 프로그래머가 양보 지점 결정
  → await 없으면 계속 실행 (블로킹 위험!)
```

---

## 실전 패턴

```python
# 1. 순차 실행 (느림)
async def sequential():
    a = await fetch_a()  # 1초
    b = await fetch_b()  # 1초
    return a, b          # 총 2초

# 2. 동시 실행 (빠름) — gather
async def concurrent():
    a, b = await asyncio.gather(
        fetch_a(),  # 1초
        fetch_b(),  # 1초
    )
    return a, b     # 총 1초!

# 3. 먼저 완료된 것부터 — as_completed
async def first_wins():
    tasks = [fetch_a(), fetch_b(), fetch_c()]
    for coro in asyncio.as_completed(tasks):
        result = await coro  # 먼저 완료된 순서대로
        print(result)

# 4. 타임아웃
async def with_timeout():
    try:
        result = await asyncio.wait_for(
            fetch_data(), timeout=5.0
        )
    except asyncio.TimeoutError:
        print("5초 초과!")

# 5. 세마포어로 동시 개수 제한
sem = asyncio.Semaphore(10)  # 최대 10개 동시
async def rate_limited_fetch(url):
    async with sem:  # 10개 초과 시 대기
        return await httpx.get(url)
```

---

## 면접 핵심 포인트

```
Q: 코루틴이 스레드보다 효율적인 이유?
A: 1. 메모리: KB vs MB (1만 개 = 수십 MB vs 수 GB)
   2. 전환 비용: ~100ns vs ~1μs (10배 차이)
   3. 동기화: 싱글 스레드라 Lock 불필요
   4. 유저 스페이스: 커널 전환 없음

Q: 코루틴의 단점?
A: 1. CPU 바운드에서 무의미 (I/O 바운드에서만 효과)
   2. 협력적이라 하나가 블로킹하면 전체 멈춤
   3. 모든 라이브러리가 async 지원해야 함 (에코시스템)
   4. 디버깅이 복잡할 수 있음 (스택 트레이스)
```
