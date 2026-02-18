# 동시성 vs 병렬성

---

## 핵심 개념

```
동시성 (Concurrency)                    병렬성 (Parallelism)
"구조적으로 동시에 다룸"                  "실제로 동시에 실행"
CPU 1개, 빠르게 전환                     CPU 여러 개, 진짜 동시 실행

시간 →                                  시간 →
[작업A][작업B][작업A][작업B]             [작업A] [작업A] [작업A]
 ↑ 번갈아 실행 (동시처럼 보임)             [작업B] [작업B] [작업B]
                                          ↑ 진짜 동시 실행
```

### 비유로 이해

```
동시성: 요리사 1명이 라면과 볶음밥을 번갈아 만듦
  → 물 끓이는 동안 (I/O 대기) 볶음밥 볶기 (다른 작업)
  → 효율적이지만 한 번에 하나만 "직접" 요리

병렬성: 요리사 2명이 동시에 각자 요리
  → 진짜 동시에 조리
  → 처리량 2배, 하드웨어(요리사) 추가 필요
```

---

## 상세 비교

| 구분 | 동시성 (Concurrency) | 병렬성 (Parallelism) |
|------|---------------------|---------------------|
| CPU | 1개면 충분 | **여러 개 필요** |
| 성격 | 구조 (Design) | 실행 (Execution) |
| 목적 | **응답성 향상** | **처리량 향상** |
| 구현 | 비동기, 코루틴, 스레드 | 멀티프로세싱, GPU |
| 적합 | **I/O 바운드** | **CPU 바운드** |
| 예시 | 웹서버 요청 처리 | 대용량 데이터 처리 |
| **Log-Doctor** | **asyncio (API 요청)** | uvicorn workers |

### I/O 바운드 vs CPU 바운드

```
I/O 바운드 (대부분의 웹 서비스):
  시간의 대부분이 I/O 대기 (네트워크, 디스크)
  
  CPU: [계산][------I/O 대기------][계산][------I/O 대기------]
  → CPU가 놀고 있음! → 동시성(비동기)으로 활용

CPU 바운드:
  시간의 대부분이 CPU 연산
  
  CPU: [계산계산계산계산계산계산계산계산계산계산]
  → CPU가 쉴 틈 없음 → 병렬성(멀티코어)으로 분산
```

### Python에서의 선택

```python
# I/O 바운드 → asyncio (동시성)
async def fetch_all_subscriptions():
    async with httpx.AsyncClient() as client:
        tasks = [client.get(url) for url in urls]
        return await asyncio.gather(*tasks)

# CPU 바운드 → multiprocessing (병렬성)
from multiprocessing import Pool

def heavy_calculation(data):
    return sum(x**2 for x in data)

with Pool(4) as pool:  # 4코어 활용
    results = pool.map(heavy_calculation, data_chunks)

# FastAPI에서의 혼합
@app.get("/api/v1/reports")
async def get_report():
    # I/O 바운드: 비동기로 처리
    raw_data = await repository.get_data()
    
    # CPU 바운드: 스레드 풀에서 처리
    report = await asyncio.to_thread(heavy_analysis, raw_data)
    return report
```

---

## 컨텍스트 스위칭 (Context Switching)

### 동작 원리

```
CPU가 현재 작업을 중단하고 다른 작업으로 전환하는 과정:

프로세스 A 실행 중
    │
    ├── 1. 인터럽트 발생 (타이머 만료, I/O 완료 등)
    │
    ├── 2. 현재 상태 저장 (PCB/TCB에 백업)
    │       ├── 프로그램 카운터 (PC): 어디까지 실행했는지
    │       ├── 레지스터 값들: 연산 중간 결과
    │       ├── 스택 포인터: 함수 호출 위치
    │       └── 페이지 테이블 포인터
    │
    ├── 3. 스케줄러가 다음 프로세스 선택
    │
    ├── 4. 선택된 프로세스 상태 복원
    │
    └── 5. 프로세스 B 실행 재개
```

### 전환 비용 비교

```
프로세스 전환 (무거움):
  - PCB 전체 저장/복원
  - 메모리 매핑 변경 (페이지 테이블)
  - TLB (Translation Lookaside Buffer) 플러시!
  - 캐시 미스 증가 (cold cache)
  → 수~수십 마이크로초

스레드 전환 (보통):
  - TCB 저장/복원 (스택, PC, 레지스터만)
  - 메모리 공간 공유 → TLB 유지 가능
  - 캐시 일부 유효
  → 1~수 마이크로초

코루틴 전환 (가벼움):
  - 함수 실행 위치만 저장 (Python generator state)
  - 커널 개입 없음 (유저 스페이스)
  - 캐시 영향 최소
  → 수백 나노초 ← FastAPI!
```

### 스케줄링 알고리즘

```
1. FCFS (First Come First Served)
   먼저 온 순서대로 처리 (비선점)
   단점: 호위 효과 (긴 작업 뒤에 짧은 작업 대기)

2. SJF (Shortest Job First)
   가장 짧은 작업 먼저 (최적이지만 실행 시간 예측 어려움)

3. Round Robin (라운드 로빈)
   각 프로세스에 동일한 시간 할당 (Time Quantum)
   → 현대 OS의 기본 방식

4. Priority Scheduling
   우선순위 높은 것 먼저
   문제: 기아 (Starvation) → 에이징(Aging)으로 해결

5. CFS (Completely Fair Scheduler)
   Linux의 기본 스케줄러
   가상 실행 시간(vruntime)이 가장 적은 프로세스에 CPU 할당
```

---

## 면접 핵심 포인트

```
Q: 동시성과 병렬성의 차이?
A: 동시성은 구조적 개념 (한 CPU에서 번갈아), 
   병렬성은 실행 개념 (여러 CPU에서 동시).
   동시성은 I/O 바운드, 병렬성은 CPU 바운드에 적합.

Q: 컨텍스트 스위칭 비용을 줄이려면?
A: 1. 스레드 > 프로세스 (공유 메모리, TLB 유지)
   2. 코루틴 > 스레드 (유저 스페이스, 커널 불개입)
   3. FastAPI: asyncio 코루틴으로 전환 비용 최소화

Q: CFS 스케줄러의 원리?
A: 각 프로세스의 가상 실행 시간(vruntime) 추적,
   vruntime이 가장 적은 프로세스에 CPU 할당.
   Red-Black 트리로 O(log n) 시간에 스케줄링.
```
