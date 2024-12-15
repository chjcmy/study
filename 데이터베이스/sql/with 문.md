~~~sql
WITH ranked_ecoli AS (
  SELECT 
    ID,
    SIZE_OF_COLONY,
    PERCENT_RANK() OVER (ORDER BY SIZE_OF_COLONY DESC) AS percentile
  FROM ECOLI_DATA
)
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
~~~

## WITH 문의 구조: "WITH CTE"

1. **W**rite the name (이름 작성)
2. **I**nsert AS keyword (AS 키워드 삽입)
3. **T**able-like query in parentheses (괄호 안에 테이블 형태의 쿼리 작성)
4. **H**ave it ready for main query (메인 쿼리에서 사용할 준비)

## CTE의 의미: "Common Table Expression"

- **C**reate temporary result (임시 결과 생성)
- **T**able-like structure (테이블과 유사한 구조)
- **E**asily referenced in main query (메인 쿼리에서 쉽게 참조)

## 사용 예시: "NAME AS (QUERY)"

```sql

WITH ExampleCTE AS (
	SELECT column1, column2
    FROM some_table    
    WHERE condition
) 
SELECT * FROM ExampleCTE;
```
## 핵심 포인트

1. 임시 테이블: WITH 문은 임시 결과셋 생성
2. 재사용성: 복잡한 쿼리 단순화 및 재사용
3. 가독성: 쿼리를 논리적 부분으로 분할

```sql

WITH MAX_SIZE_PER_YEAR AS (
	SELECT YEAR(DIFFERENTIATION_DATE) AS YEAR, MAX(SIZE_OF_COLONY) AS MAX_SIZE 
	FROM ECOLI_DATA 
	GROUP BY YEAR(DIFFERENTIATION_DATE) 
	) 
	SELECT YEAR(E.DIFFERENTIATION_DATE) AS YEAR, M.MAX_SIZE - E.SIZE_OF_COLONY AS YEAR_DEV, E.ID 
	FROM ECOLI_DATA E 
	JOIN MAX_SIZE_PER_YEAR M ON YEAR(E.DIFFERENTIATION_DATE) = M.YEAR 
	ORDER BY YEAR, YEAR_DEV;
```

## 관련 태그

#SQL #CTE #DatabaseQuery #QueryOptimization
