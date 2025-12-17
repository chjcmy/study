# [HackerRank] Occupations
#Database/SQL/ProblemSolving

> [!NOTE] 문제
> `OCCUPATIONS` 테이블의 `Occupation` 열을 피벗(Pivot)하여, 각 직업(Doctor, Professor, Singer, Actor)에 해당하는 이름들을 알파벳 순서로 정렬하여 세로로 출력하는 문제입니다.

## 최종 쿼리

```sql
-- 각 직업별로 이름에 순번(row_number)을 매기는 CTE를 정의
WITH base AS (
    SELECT
        *,
        ROW_NUMBER() OVER (PARTITION BY occupation ORDER BY name) AS R
    FROM occupations
)
-- 순번(R)을 기준으로 그룹화하고, 각 직업에 해당하는 이름을 Pivot하여 출력
SELECT
    MAX(IF(occupation = 'Doctor', name, NULL)) AS Doctor,
    MAX(IF(occupation = 'Professor', name, NULL)) AS Professor,
    MAX(IF(occupation = 'Singer', name, NULL)) AS Singer,
    MAX(IF(occupation = 'Actor', name, NULL)) AS Actor
FROM base
GROUP BY R;
```

## 핵심 로직

1.  **`ROW_NUMBER() OVER (PARTITION BY ...)`**: `PARTITION BY occupation`을 사용하여 각 직업 그룹 내에서 `ORDER BY name`으로 이름의 순번을 매깁니다. 이렇게 하면 모든 직업 그룹에 대해 동일한 순번(R)을 가진 이름들이 같은 행에 위치할 수 있게 됩니다.
2.  **`MAX(IF(condition, ...))`**: `GROUP BY R`을 통해 같은 순번(R)을 가진 데이터들을 하나의 행으로 묶습니다. 이 때, `IF` 문을 사용하여 자신의 직업 컬럼이 아닐 경우 `NULL`을 반환하고, `MAX` 함수를 통해 각 행에서 `NULL`이 아닌 실제 이름 값만 추출하여 최종 결과를 만듭니다. (이 경우 `MAX`는 단순히 `NULL`을 무시하고 값을 가져오는 역할을 합니다.)

---
> [[00. 데이터베이스 목차.md|⬆️ 목차로 돌아가기]]