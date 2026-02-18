# 파티셔닝, 샤딩, 커넥션 풀

---

## 파티셔닝 (Partitioning)

큰 테이블을 여러 개의 물리적 공간으로 나누는 것.

### 1. 수직 파티셔닝 (Vertical)
*   특정 **컬럼**을 쪼갬. (정규화와 유사하지만 성능 목적)
*   예: `User` 테이블이 너무 클 때
    *   `User_Core`: id, password, name (로그인용, 자주 사용)
    *   `User_Detail`: id, address, bio, photo (마이페이지용)
    *   → 자주 쓰는 Core 테이블만 메모리에 올려 성능 향상.

### 2. 수평 파티셔닝 (Horizontal) = 샤딩 (Sharding)
*   **행(Row)**을 쪼갬.
*   데이터 개수가 10억 건 넘어갈 때 여러 서버/테이블로 분산.

---

## 샤딩 (Sharding) 전략

어떤 기준으로 데이터를 나눌 것인가? (Sharding Key 선택)

### 1. Range Sharding (범위)
*   `id` 1~100만 → A서버, 100만~200만 → B서버
*   장점: 구현 쉬움, 범위 조회 유리.
*   단점: 최신 데이터(최근 id)에 트래픽 쏠림 (Hot Spot).

### 2. Hash Sharding (해시)
*   `id % 2 == 0` → A서버, `1` → B서버
*   장점: 데이터가 균등하게 분산됨.
*   단점: 서버 확장 시(2대→3대), 해시 다시 계산해서 대이동 필요 (Rebalancing).

### 3. Directory Sharding
*   별도의 참조 테이블(Look-up Table)에 `id: 서버위치` 매핑 저장.
*   장점: 유연함. 특정 데이터만 이사 가능.
*   단점: 참조 테이블이 성능 병목이 됨 (SPOF 가능성).

---

## N+1 문제

### 문제 정의

ORM(JPA, Django, SQLAlchemy) 사용 시 흔한 성능 이슈.
"목록을 조회(1)하고, 각 항목의 연관 데이터를 조회(N)하느라 쿼리가 N+1번 나가는 현상."

### 예시 (블로그)

```python
# 1. 글 목록 조회 (쿼리 1번)
posts = Post.objects.all() 

for post in posts:
    # 2. 각 글의 작성자 이름 출력 (쿼리 N번 추가 실행!)
    print(post.author.name) 

# 글이 100개면 → 1 + 100 = 101번 쿼리 실행!
# DB 부하 급증.
```

### 해결 방법

1.  **Fetch Join (Eager Loading):**
    *   처음부터 JOIN 쿼리로 한 번에 가져옴 (`SELECT ... FROM Post JOIN Author ...`)
    *   Django: `select_related`, `prefetch_related`
    *   JPA: `join fetch`
2.  **Batch Size:**
    *   IN 쿼리로 묶어서 조회 (`WHERE author_id IN (1, 2, 3...)`)

---

## 커넥션 풀 (Connection Pool)

### 필요성

*   DB 연결(TCP 3-way handshake + 인증)은 매우 비싼 작업.
*   매 요청마다 연결/해제를 반복하면 성능 저하 심각.

### 동작 원리

1.  미리 N개의 DB 연결을 만들어 **Pool**에 담아둠.
2.  요청이 오면 Pool에서 빌려줌.
3.  다 쓰면 Pool에 반납 (연결 종료 X).

### 주의사항

*   **Max Connection:** 너무 크게 잡으면 DB 메모리 부족. 너무 작으면 대기 시간 증가.
*   서버가 여러 대면 (서버 수 × 풀 크기)가 DB 허용량을 넘지 않도록 주의.

---

## 면접 핵심 포인트

```
Q: 샤딩(Sharding)이란?
A: 데이터를 여러 DB 서버에 수평 분산 저장하는 기술.
   Range, Hash 방식 등이 있으며, 데이터 분산 균형과 쿼리 효율성을 고려해 샤딩 키를 잘 정해야 함.

Q: N+1 문제 해결법?
A: ORM의 Lazy Loading 때문에 발생하므로, Eager Loading(Fetch Join)을 사용하여
   한 번의 쿼리(또는 IN 쿼리)로 필요한 데이터를 미리 가져와야 함.

Q: 커넥션 풀을 쓰는 이유?
A: DB 연결 비용(Handshake)을 절약하여 응답 속도를 높이기 위함.
   미리 생성된 연결을 재사용하여 처리량을 극대화함.
```
