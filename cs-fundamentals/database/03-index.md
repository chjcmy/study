# 데이터베이스 인덱스 (Index)

---

## 인덱스란?

```
책의 "색인(찾아보기)"과 같음.
데이터를 빨리 찾기 위해 별도로 구성한 자료구조.

장점: 검색 속도(SELECT)가 엄청나게 빨라짐 (O(N) → O(log N))
단점: 1. 추가 저장 공간 필요 (약 10~30%)
      2. 쓰기(INSERT, UPDATE, DELETE) 속도 저하 (인덱스도 갱신해야 하므로)
```

---

## 자료구조: B-Tree vs B+ Tree

대부분의 MySQL/PostgreSQL은 **B+ Tree**를 사용.

### B-Tree
*   노드에 데이터도 함께 저장.
*   모든 리프 노드를 탐색하지 않아도 됨.
*   범위 검색에 불리.

### B+ Tree (표준) ⭐
*   **리프 노드에만** 실제 데이터를 저장 (혹은 포인터).
*   모든 리프 노드가 **Linked List**로 연결됨.
*   **범위 검색(Range Scan)**에 매우 효율적! (`BETWEEN`, `>`, `<`)

---

## 클러스터드 vs 넌-클러스터드

| 구분 | Clustered Index | Non-Clustered Index (Secondary) |
|------|-----------------|---------------------------------|
| **개수** | 테이블당 **1개** (보통 PK) | 여러 개 가능 |
| **저장** | 데이터 자체가 정렬되어 저장됨 | 별도의 인덱스 페이지 생성 |
| **속도** | 읽기 매우 빠름 (바로 데이터) | 읽기 후 데이터 페이지 조회 필요 |
| **비유** | 사전 (내용 자체가 순서대로) | 일반 책의 색인 (페이지 번호 참조) |

```sql
-- 클러스터드 인덱스 (PK)
CREATE TABLE users (
    id BIGINT PRIMARY KEY, -- id 기준으로 데이터가 물리적으로 정렬됨
    name VARCHAR(255)
);

-- 넌-클러스터드 인덱스
CREATE INDEX idx_name ON users(name); 
-- name 순서로 정렬된 별도 공간 생성. id(PK)를 가리킴.
```

---

## 복합 인덱스 (Composite Index)

여러 컬럼을 묶어서 인덱스 생성. **순서가 매우 중요!**

```sql
CREATE INDEX idx_a_b ON table(a, b);

-- ✅ 인덱스 타는 경우 (Leftmost Prefix Rule)
SELECT * FROM table WHERE a = 1;
SELECT * FROM table WHERE a = 1 AND b = 2;

-- ❌ 인덱스 안 타는 경우
SELECT * FROM table WHERE b = 2; -- a 없이 b만 찾으면 인덱스 무용지물
```

---

## 인덱스 스캔 방식

1.  **Index Seek:** 특정 값을 콕 집어 찾음 (가장 빠름)
2.  **Index Scan:** 인덱스 전체를 흝음 (테이블 풀스캔보단 빠르지만 느림)
3.  **Table Full Scan:** 인덱스 없이 모든 데이터 뒤짐 (최악)

---

## 면접 핵심 포인트

```
Q: 인덱스를 많이 만들면 좋은가?
A: 아니오. 읽기 성능은 오르지만, 쓰기(INSERT/UPDATE) 성능이 떨어지고 저장 공간을 차지합니다.
   조회 패턴을 분석해 필요한 곳에만 생성해야 합니다.

Q: B+ Tree를 사용하는 이유는?
A: 리프 노드끼리 연결되어 있어 '범위 검색(Range Scan)'에 매우 유리하기 때문입니다.
   DB는 범위 조회가 빈번하므로 Hash Index(O(1)이지만 범위 검색 불가)보다 B+ Tree를 선호합니다.

Q: 복합 인덱스 (A, B)에서 WHERE B = 1 조회 시 인덱스를 타나요?
A: 타지 않습니다(Leftmost Prefix Rule 위배). 인덱스는 A 기준으로 정렬되고, A가 같을 때 B가 정렬된 구조입니다.
   따라서 선행 컬럼(A) 없이 후행 컬럼(B)만으로는 탐색이 불가능합니다.
```
