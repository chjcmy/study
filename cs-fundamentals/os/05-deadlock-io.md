# 데드락 & I/O 모델

---

## 데드락 (Deadlock)

### 정의와 예시

```
스레드 A: Lock 1 획득 →                Lock 2 대기 (B가 가지고 있음)
스레드 B:                Lock 2 획득 → Lock 1 대기 (A가 가지고 있음)
→ 서로 영원히 대기 = 데드락!
```

### 데드락 발생 필요충분 조건 (4가지 모두 충족 시)

```
1. 상호 배제 (Mutual Exclusion)
   → 자원을 한 번에 하나의 프로세스만 사용

2. 점유 대기 (Hold and Wait)
   → 자원을 가진 채로 다른 자원을 대기

3. 비선점 (No Preemption)
   → 다른 프로세스의 자원을 강제로 빼앗을 수 없음

4. 순환 대기 (Circular Wait)
   → A→B→C→A 형태로 자원 대기가 순환

→ 4가지 중 하나만 깨면 데드락 방지!
```

### 데드락 해결 전략

```
1. 예방 (Prevention)
   4가지 조건 중 하나를 원천 차단
   - 순환 대기 깨기: 락 획득 순서를 정함
     ✅ 항상 Lock 1 → Lock 2 순서로
     ❌ 스레드마다 다른 순서 X

2. 회피 (Avoidance)
   은행원 알고리즘: 안전 상태인지 사전 확인
   "이 자원을 줘도 안전한가?" → 안전하면 할당, 위험하면 대기

3. 감지 & 회복 (Detection & Recovery)
   주기적으로 Wait-for-Graph 검사
   순환 감지 시 → 프로세스 종료 또는 자원 회수

4. 무시 (Ostrich Algorithm)
   데드락 확률이 극히 낮으면 무시 (대부분의 OS가 채택)
   발생 시 재시작
```

### 실무 예시

```python
# ❌ 데드락 가능
import threading

lock_a = threading.Lock()
lock_b = threading.Lock()

def task_1():
    lock_a.acquire()
    lock_b.acquire()  # task_2가 lock_b 가지고 있으면 데드락!
    # ...
    lock_b.release()
    lock_a.release()

def task_2():
    lock_b.acquire()
    lock_a.acquire()  # task_1이 lock_a 가지고 있으면 데드락!
    # ...

# ✅ 해결: 락 순서 통일
def task_1():
    lock_a.acquire()  # 항상 a → b 순서
    lock_b.acquire()

def task_2():
    lock_a.acquire()  # 항상 a → b 순서 (동일!)
    lock_b.acquire()

# ✅ 해결: 타임아웃 사용
if lock_a.acquire(timeout=5):
    if lock_b.acquire(timeout=5):
        # 작업
    else:
        lock_a.release()  # 실패 시 해제
```

### 라이브락과 기아

```
라이브락 (Livelock):
  데드락과 비슷하지만 프로세스가 "계속 동작은 함"
  두 사람이 좁은 복도에서 양보하며 계속 같은 방향으로 비키는 상황
  → 무한히 상태 변경만 하고 진전 없음

기아 (Starvation):
  특정 프로세스가 영원히 자원을 얻지 못함
  → 해결: 에이징(Aging) — 대기 시간에 비례해 우선순위 상승
```

---

## I/O 모델

### 4가지 I/O 모델

```
                    블로킹              논블로킹
                    (작업 끝날 때까지)    (바로 리턴)
              ┌────────────────┬────────────────┐
동기          │ Sync-Blocking  │ Sync-Non-Block │
(직접 확인)   │ read() 호출    │ poll/select    │
              │ → 완료까지 멈춤 │ → 반복 확인    │
              ├────────────────┼────────────────┤
비동기        │ (의미 없거나    │ Async-Non-Block│
(OS가 알려줌) │  구현 안 함)    │ epoll, IOCP   │
              │                │ asyncio ⭐      │
              └────────────────┴────────────────┘
```

### 각 모델 상세

```
1. Sync + Blocking (가장 단순)
   앱: read(fd) 호출
   커널: 데이터 준비될 때까지 앱 블록
   커널: 데이터 복사 완료 → 앱에 반환
   → requests.get() 가 대표적

2. Sync + Non-Blocking (폴링)
   앱: read(fd) 호출 (O_NONBLOCK)
   커널: "아직 안됐어" (EAGAIN)
   앱: 다시 read(), 다시 read()... (busy waiting)
   커널: "됐어!" → 데이터 반환
   → CPU 낭비가 심함

3. I/O Multiplexing (select/poll/epoll)
   앱: select([fd1, fd2, fd3]) "이 중에 준비된 거 알려줘"
   커널: fd2 준비됨!
   앱: read(fd2) → 데이터 받음
   → 하나의 스레드로 여러 I/O 관리

4. Async + Non-Blocking (비동기 I/O) ⭐
   앱: aio_read(fd, callback) "끝나면 알려줘"
   커널: 데이터 준비 + 복사까지 완료 → 앱에 알림
   앱: 그 동안 다른 일 수행 가능!
   → Linux: io_uring, epoll + userspace (Python asyncio)
```

### epoll (Linux 핵심)

```
select의 문제:
  매 호출마다 모든 fd를 커널에 전달 → O(n) 스캔

epoll의 해결:
  1. epoll_create(): 관심 fd 집합 생성
  2. epoll_ctl(): fd 등록/수정/삭제
  3. epoll_wait(): 이벤트 발생한 fd만 반환 → O(1)!

  → 수만 개 동시 연결 처리 가능 (C10K 문제 해결)

FastAPI/uvloop:
  uvloop = libuv (Node.js의 이벤트 루프) 기반
  내부적으로 epoll 사용
  → Python asyncio보다 2~4배 빠름
```

---

## 면접 핵심 포인트

```
Q: 데드락 발생 조건과 해결법?
A: 4조건(상호배제, 점유대기, 비선점, 순환대기) 모두 충족 시 발생.
   해결: 예방(순서 통일), 회피(은행원), 감지(그래프), 무시(재시작).
   실무: 락 순서 통일 + 타임아웃이 일반적.

Q: epoll과 select의 차이?
A: select: 모든 fd를 매번 전달, O(n) 스캔, fd 1024개 제한.
   epoll: 커널이 관심 fd를 관리, 이벤트 발생 fd만 반환 O(1).
   → 대량 동시 연결(C10K+) 처리에 epoll 필수.

Q: FastAPI의 I/O 모델?
A: 비동기 + 논블로킹 (epoll 기반 uvloop/asyncio).
   I/O 대기 중 이벤트 루프가 다른 코루틴 실행.
   싱글 스레드로 수만 동시 연결 처리 가능.
```

> FastAPI의 `async/await` = **비동기 + 논블로킹 I/O** (epoll 기반 uvloop)
