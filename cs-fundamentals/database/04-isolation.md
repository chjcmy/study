# 트랜잭션 격리 수준 (Isolation Level)

---

## 트랜잭션의 ACID

1.  **Atomicity (원자성):** All or Nothing. (모두 성공하거나 모두 실패)
2.  **Consistency (일관성):** 트랜잭션 전후 데이터 무결성 유지.
3.  **Isolation (격리성):** 여러 트랜잭션이 서로 간섭하지 않음.
4.  **Durability (지속성):** 커밋된 데이터는 영구 저장.

---

## 동시성 문제 (Concurrency Problems)

여러 트랜잭션이 동시에 실행될 때 발생하는 문제들:

1.  **Dirty Read:** 커밋되지 않은 데이터를 다른 트랜잭션이 읽음.
    *   A가 수정 중인데 B가 읽음 → A가 롤백하면 B는 잘못된 데이터를 가진 셈.
2.  **Non-Repeatable Read:** 같은 쿼리를 두 번 했는데 값이 다름.
    *   A가 조회 → B가 **수정(UPDATE)** 및 커밋 → A가 다시 조회 (값이 바뀜!)
3.  **Phantom Read:** 같은 쿼리를 두 번 했는데 없던 데이터가 생김(유령).
    *   A가 범위 조회 → B가 **삽입(INSERT)** 및 커밋 → A가 다시 조회 (행 개수가 늘어남!)

---

## 격리 수준 4단계 (표준)

아래로 갈수록 격리 수준은 높아지지만(데이터 안전), **성능(동시성)은 떨어진다.**

| 수준 | Dirty Read | Non-Repeatable | Phantom Read | 특징 |
|------|------------|----------------|--------------|------|
| **1. Read Uncommitted** | 발생 ⭕ | 발생 ⭕ | 발생 ⭕ | 거의 안 씀. 빠르지만 위험. |
| **2. Read Committed** | 방지 ✅ | 발생 ⭕ | 발생 ⭕ | **PostgreSQL, Oracle, SQL Server 기본값**. 가장 많이 사용. |
| **3. Repeatable Read** | 방지 ✅ | 방지 ✅ | 발생 ⭕ | **MySQL(InnoDB) 기본값**. 트랜잭션 내 일관된 읽기 보장. |
| **4. Serializable** | 방지 ✅ | 방지 ✅ | 방지 ✅ | 완벽 격리. 성능 최악 (거의 1줄 서기). |

---

## MVCC (Multi-Version Concurrency Control)

```
"락(Lock) 없이 읽기 일관성을 보장하는 기술"

원리: 데이터를 덮어쓰지 않고, 새로운 버전(스냅샷)을 생성.
- 트랜잭션 A가 데이터를 수정 중이어도,
- 트랜잭션 B는 수정 전의 '과거 버전' 데이터를 읽음. (대기하지 않음!)

→ Read Committed, Repeatable Read 구현의 핵심 기술.
→ MySQL(Undo Log), PostgreSQL, Oracle 모두 사용.
```

---

## 면접 핵심 포인트

```
Q: 격리 수준을 높이면 무조건 좋은가?
A: 데이터 정합성은 좋아지지만 동시 처리량(Throughput)이 급격히 떨어지고 데드락 가능성이 높아집니다.
   대부분 Read Committed나 Repeatable Read를 사용하고, 민감한 정합성은 낙관적 락(Optimistic Lock) 등으로 애플리케이션 레벨에서 풉니다.

Q: Repeatable Read와 Read Committed의 차이는?
A: Read Committed는 트랜잭션 중에도 다른 트랜잭션의 '커밋된 변경사항'을 읽습니다(값이 바뀔 수 있음).
   Repeatable Read는 트랜잭션 시작 시점의 스냅샷을 보므로, 트랜잭션 내내 항상 같은 값을 읽습니다.

Q: MVCC란?
A: 다중 버전 동시성 제어. 락을 걸지 않고도 읽기/쓰기를 병행할 수 있게 해줍니다.
   쓰기 트랜잭션이 있어도 읽기 트랜잭션은 이전 버전을 읽으므로 대기하지 않습니다(Blocking X).
```
