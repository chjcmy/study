# 7. 트랜잭션과 동시성 제어
#PostgreSQL/Transaction #PostgreSQL/Concurrency

여러 사용자가 동시에 데이터베이스에 접근할 때 데이터의 일관성과 무결성을 지키는 것은 매우 중요합니다. PostgreSQL은 이를 위해 트랜잭션(Transaction), ACID 원칙, 그리고 정교한 동시성 제어(Concurrency Control) 메커니즘을 제공합니다.

## 7.1. 트랜잭션과 ACID

### 트랜잭션(Transaction)이란?

> [!INFO]
> 트랜잭션은 **"더 이상 쪼갤 수 없는 데이터베이스 작업의 단위"**입니다. 여러 개의 SQL 명령어를 하나의 논리적 단위로 묶은 것으로, 이 단위 내의 모든 작업은 전부 성공하거나(`COMMIT`) 전부 실패(`ROLLBACK`)해야 합니다.

```sql
BEGIN; -- 트랜잭션 시작

UPDATE accounts SET balance = balance - 100 WHERE user_id = 1;
UPDATE accounts SET balance = balance + 100 WHERE user_id = 2;

COMMIT; -- 모든 변경사항을 최종 적용
-- 만약 중간에 오류가 발생하면 ROLLBACK; 명령으로 모든 변경사항을 취소
```

### ACID 원칙

ACID는 데이터베이스 트랜잭션이 가져야 할 4가지 핵심 속성입니다.

1.  **원자성 (Atomicity)**: 트랜잭션은 하나의 원자처럼 취급됩니다. 모든 작업이 성공적으로 완료되거나, 하나라도 실패하면 모든 작업이 없던 일이 됩니다.
2.  **일관성 (Consistency)**: 트랜잭션이 성공적으로 완료되면 데이터베이스는 항상 일관된 상태를 유지해야 합니다. (예: 계좌 이체 후 두 계좌의 총액은 이전과 동일해야 함)
3.  **고립성 (Isolation)**: 여러 트랜잭션이 동시에 실행될 때, 각 트랜잭션은 다른 트랜잭션의 작업에 영향을 받지 않고 독립적으로 실행되는 것처럼 보여야 합니다. (아래 '격리 수준'에서 자세히 설명)
4.  **지속성 (Durability)**: 성공적으로 완료된 트랜잭션의 결과는 시스템에 장애가 발생하더라도 영구적으로 저장되어야 합니다. PostgreSQL은 이를 위해 WAL(Write-Ahead Logging) 메커니즘을 사용합니다.

## 7.2. 다중 버전 동시성 제어 (MVCC)

PostgreSQL은 높은 동시성을 처리하기 위해 **MVCC(Multi-Version Concurrency Control)** 라는 메커니즘을 사용합니다.

-   **동작 방식**: 데이터를 `UPDATE`하거나 `DELETE`할 때, 기존 데이터를 직접 덮어쓰거나 지우지 않고, 데이터의 새로운 '버전'을 만듭니다. 각 트랜잭션은 자신이 시작된 시점을 기준으로, 자신에게만 보이는 특정 버전의 데이터(스냅샷)를 읽게 됩니다.
-   **장점**: 읽기 작업이 쓰기 작업을 막지 않고, 쓰기 작업 또한 읽기 작업을 막지 않습니다 (`Reading is non-blocking`). 이를 통해 전통적인 잠금(Lock) 방식보다 훨씬 높은 동시 처리 성능을 얻을 수 있습니다.
-   **`VACUUM`**: MVCC로 인해 쌓이는 옛날 버전의 데이터(Dead Tuple)는 `VACUUM` 프로세스를 통해 주기적으로 정리되어야 합니다.

## 7.3. 트랜잭션 격리 수준 (Isolation Levels)

고립성(Isolation)을 어느 정도로 보장할 것인지를 정하는 단계입니다. 격리 수준이 높아질수록 데이터 정합성은 높아지지만, 동시 처리 성능은 떨어질 수 있습니다.

-   **`READ COMMITTED` (기본값)**: 트랜잭션 내에서 쿼리를 실행할 때마다 **그 시점에 커밋된** 데이터만 읽습니다. 한 트랜잭션 내에서도 쿼리를 실행하는 시점마다 다른 결과를 얻을 수 있습니다 (Non-Repeatable Read).
-   **`REPEATABLE READ`**: 트랜잭션이 **시작된 시점**을 기준으로 데이터를 읽습니다. 트랜잭션 내내 동일한 데이터를 조회함을 보장합니다 (Repeatable Read). 다른 트랜잭션이 데이터를 수정했더라도, 현재 트랜잭션에게는 보이지 않습니다.
-   **`SERIALIZABLE`**: 가장 엄격한 격리 수준. 여러 트랜잭션이 동시에 실행되더라도, 마치 순서대로 하나씩 실행된 것과 동일한 결과를 보장합니다. 동시성 관련 버그를 가장 확실하게 막을 수 있지만 성능 저하가 가장 큽니다.

```sql
-- 트랜잭션의 격리 수준 설정
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ;
-- 또는
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
```

## 7.4. 잠금 (Lock)

MVCC가 대부분의 상황을 처리하지만, 여러 트랜잭션이 동일한 데이터를 동시에 수정하려는 경쟁 상태(Race Condition)를 막기 위해서는 잠금(Lock)이 필요합니다.

-   **자동 잠금**: `UPDATE`, `DELETE` 등의 DML 명령어는 대상이 되는 행(Row)에 자동으로 배타적인 잠금(Exclusive Lock)을 겁니다. 다른 트랜잭션은 이 잠금이 풀릴 때까지 해당 행을 수정하거나 삭제할 수 없습니다.
-   **명시적 잠금 (`SELECT FOR ...`)**: 개발자가 필요에 따라 특정 행을 명시적으로 잠글 수 있습니다.
    > [!TIP] 비관적 잠금 (Pessimistic Locking)
    > -   **`SELECT ... FOR UPDATE`**: 특정 행을 조회하면서 즉시 배타적인 잠금을 겁니다. 다른 트랜잭션은 이 행에 접근하여 `UPDATE`, `DELETE` 하거나 `FOR UPDATE` 잠금을 걸 수 없으며, 현재 트랜잭션이 `COMMIT` 또는 `ROLLBACK` 될 때까지 기다려야 합니다. 동시 수정으로 인한 데이터 덮어쓰기를 막을 때 유용합니다.
    > -   **`SELECT ... FOR SHARE`**: 공유 잠금(Shared Lock)을 겁니다. 다른 트랜잭션이 이 행을 `UPDATE`하거나 `DELETE`하는 것은 막지만, `FOR SHARE` 잠금을 거는 것은 허용합니다.

```sql
BEGIN;

-- user_id가 1인 사용자의 포인트를 조회하고, 다른 트랜잭션이 수정하지 못하도록 잠금을 건다.
SELECT points FROM users WHERE user_id = 1 FOR UPDATE;

-- (애플리케이션 로직으로 포인트 계산)

-- 계산된 결과로 포인트 업데이트
UPDATE users SET points = 150 WHERE user_id = 1;

COMMIT;
```

---
> [[00. 포스트그레스 목차|⬆️ 목차로 돌아가기]]
> [[6. 인덱스/README|⬅️ 이전: 6. 인덱스]] | [[8. 성능 튜닝/README|➡️ 다음: 8. 성능 튜닝]]