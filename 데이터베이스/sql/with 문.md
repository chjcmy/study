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


