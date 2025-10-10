# WITH 문 (Common Table Expression)
#Database/SQL #SQL/CTE

> [!INFO] WITH 문 (CTE)이란?
> `WITH` 문은 **CTE(Common Table Expression, 공통 테이블 표현식)**를 정의하는 데 사용됩니다. CTE는 복잡한 쿼리 내에서 한 번 이상 사용되거나, 쿼리 자체를 더 논리적이고 읽기 쉽게 만들기 위해 사용하는 **이름을 가진 임시 결과 집합(temporary result set)**입니다.

## CTE의 장점

-   **가독성 향상**: 긴 쿼리를 여러 논리적 단위로 나눌 수 있어, 쿼리의 의도를 파악하기 쉬워집니다.
-   **재사용성**: 하나의 CTE를 정의한 뒤, 메인 쿼리에서 여러 번 참조할 수 있습니다.
-   **재귀 쿼리 작성**: 계층적인 데이터를 쿼리할 때 `WITH RECURSIVE` 구문을 사용하여 강력한 재귀 쿼리를 작성할 수 있습니다.

## 기본 구조

```sql
WITH [CTE 이름] AS (
    -- 이 부분에 서브쿼리 작성
    SELECT column1, column2
    FROM some_table
    WHERE condition
)
-- 메인 쿼리에서 정의된 CTE를 테이블처럼 사용
SELECT *
FROM [CTE 이름];
```

## 사용 예시

### 예시 1: 순위 매기기

```sql
-- 각 대장균 개체의 크기를 기준으로 상위 백분율 순위를 매기는 CTE 정의
WITH ranked_ecoli AS (
  SELECT
    ID,
    SIZE_OF_COLONY,
    PERCENT_RANK() OVER (ORDER BY SIZE_OF_COLONY DESC) AS percentile
  FROM ECOLI_DATA
)
-- 정의된 CTE를 사용하여 등급(COLONY_NAME) 부여
SELECT
  ID,
  CASE
    WHEN percentile <= 0.25 THEN 'CRITICAL'
    WHEN percentile <= 0.50 THEN 'HIGH'
    WHEN percentile <= 0.75 THEN 'MEDIUM'
    ELSE 'LOW'
  END AS COLONY_NAME
FROM ranked_ecoli
ORDER BY ID ASC;
```

### 예시 2: 연도별 최대값과 비교

```sql
-- 연도별 최대 대장균 크기를 계산하는 CTE 정의
WITH MAX_SIZE_PER_YEAR AS (
    SELECT
        YEAR(DIFFERENTIATION_DATE) AS YEAR,
        MAX(SIZE_OF_COLONY) AS MAX_SIZE
    FROM ECOLI_DATA
    GROUP BY YEAR(DIFFERENTIATION_DATE)
)
-- 메인 쿼리에서 원본 테이블과 CTE를 조인하여 연도별 편차 계산
SELECT
    YEAR(E.DIFFERENTIATION_DATE) AS YEAR,
    (M.MAX_SIZE - E.SIZE_OF_COLONY) AS YEAR_DEV,
    E.ID
FROM ECOLI_DATA E
JOIN MAX_SIZE_PER_YEAR M ON YEAR(E.DIFFERENTIATION_DATE) = M.YEAR
ORDER BY YEAR, YEAR_DEV;
```

---
> [[00. 데이터베이스 목차.md|⬆️ 목차로 돌아가기]]