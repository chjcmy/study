# 4. 고급 SQL (Advanced SQL)
#PostgreSQL/SQL/Advanced

기본 SQL에 익숙해졌다면, 더 복잡하고 강력한 데이터 조작 및 분석을 위해 고급 SQL 기능들을 배워야 합니다. 이 단원에서는 여러 테이블을 연결하는 `JOIN`, 쿼리 안의 쿼리인 `서브쿼리`, 복잡한 순위와 분석을 위한 `윈도우 함수`, 그리고 쿼리의 가독성을 높이는 `CTE`에 대해 알아봅니다.

---

## 4.1. JOIN

> [!INFO] JOIN이란?
> `JOIN`은 두 개 이상의 테이블에 나뉘어 저장된 관련 데이터를 하나의 결과로 묶어주는 핵심적인 기능입니다.

*   **`INNER JOIN`**: 두 테이블 모두에 조인 조건이 일치하는 데이터만 반환합니다.
*   **`LEFT JOIN`** (or `LEFT OUTER JOIN`)**: 왼쪽 테이블의 모든 데이터를 반환하고, 오른쪽 테이블은 조인 조건에 일치하는 데이터만 붙여줍니다. 일치하는 데이터가 없으면 `NULL`이 됩니다.
*   **`RIGHT JOIN`** (or `RIGHT OUTER JOIN`)**: `LEFT JOIN`의 반대. 오른쪽 테이블의 모든 데이터를 반환합니다.
*   **`FULL OUTER JOIN`**: 양쪽 테이블의 모든 데이터를 반환합니다. 조인 조건에 일치하지 않는 부분은 `NULL`로 채워집니다.

**예제용 테이블:**
```sql
CREATE TABLE employees (
    id INT PRIMARY KEY,
    name VARCHAR(50),
    dept_id INT
);
CREATE TABLE departments (
    id INT PRIMARY KEY,
    dept_name VARCHAR(50)
);
INSERT INTO employees VALUES (1, 'Alice', 1), (2, 'Bob', 2), (3, 'Charlie', NULL);
INSERT INTO departments VALUES (1, 'Engineering'), (2, 'Marketing'), (3, 'Sales');
```

**`INNER JOIN` 예제:**
```sql
-- 직원의 이름과 그 직원이 속한 부서의 이름을 조회
SELECT e.name, d.dept_name
FROM employees e
INNER JOIN departments d ON e.dept_id = d.id;
-- 결과: 'Alice'와 'Bob'만 조회됨 (부서가 없는 Charlie는 제외)
```

**`LEFT JOIN` 예제:**
```sql
-- 모든 직원의 이름과 그들이 속한 부서의 이름을 조회 (부서가 없어도 직원은 모두 표시)
SELECT e.name, d.dept_name
FROM employees e
LEFT JOIN departments d ON e.dept_id = d.id;
-- 결과: 'Alice', 'Bob', 'Charlie' 모두 조회되며, Charlie의 dept_name은 NULL
```

---

## 4.2. 서브쿼리 (Subquery)

> [!INFO] 서브쿼리란?
> 서브쿼리는 다른 SQL 쿼리 내에 포함된 `SELECT` 문입니다. 복잡한 조건을 만들거나, 다른 쿼리의 결과를 임시 테이블처럼 사용할 때 유용합니다.

**`WHERE` 절에서 사용:**
```sql
-- 'Engineering' 부서에 속한 직원의 이름을 조회
SELECT name
FROM employees
WHERE dept_id = (SELECT id FROM departments WHERE dept_name = 'Engineering');
```

**`FROM` 절에서 사용 (인라인 뷰):**
```sql
-- 각 부서의 직원 수를 계산한 임시 테이블(인라인 뷰)을 만들고, 그 결과를 조회
SELECT d.dept_name, emp_counts.count
FROM departments d
JOIN (
    SELECT dept_id, COUNT(*) as count
    FROM employees
    GROUP BY dept_id
) AS emp_counts ON d.id = emp_counts.dept_id;
```

---

## 4.3. 윈도우 함수 (Window Functions)

> [!INFO] 윈도우 함수란?
> 윈도우 함수는 `GROUP BY`처럼 행을 그룹화하지 않으면서, 각 행에 대해 순위, 누적 합계, 평균 등을 계산할 수 있는 강력한 기능입니다. `OVER()` 구문과 함께 사용됩니다.

**주요 윈도우 함수:**
*   `ROW_NUMBER()`: 파티션 내에서 각 행에 고유한 순번을 부여합니다.
*   `RANK()`, `DENSE_RANK()`: 순위를 부여합니다. (`RANK`는 동점자 다음 순위를 건너뛰고, `DENSE_RANK`는 건너뛰지 않습니다.)
*   `SUM()`, `AVG()`, `COUNT()`: 파티션을 나누어 집계 계산을 합니다.
*   `LAG()`, `LEAD()`: 현재 행을 기준으로 이전 또는 다음 행의 값에 접근합니다.

**예제용 테이블:**
```sql
CREATE TABLE sales (
    employee_name VARCHAR(50),
    sale_amount INT
);
INSERT INTO sales VALUES ('Alice', 300), ('Bob', 400), ('Alice', 500), ('Charlie', 200), ('Bob', 400);
```

**`RANK()` 예제:**
```sql
-- 판매액(sale_amount) 기준으로 직원의 판매 순위를 매김
SELECT
    employee_name,
    sale_amount,
    RANK() OVER (ORDER BY sale_amount DESC) as sale_rank
FROM sales;
-- 결과: Bob(400)은 공동 2위, Alice(300)는 4위가 됨 (3위는 건너뜀)
```

**파티션과 함께 사용:**
```sql
-- 각 직원별로 판매액 순위를 매김
SELECT
    employee_name,
    sale_amount,
    RANK() OVER (PARTITION BY employee_name ORDER BY sale_amount DESC) as sale_rank_per_employee
FROM sales;
-- 결과: Alice의 판매 기록 내에서 순위가 매겨지고, Bob의 기록 내에서도 별도로 순위가 매겨짐
```

---

## 4.4. 공통 테이블 표현식 (CTE - Common Table Expressions)

> [!INFO] CTE란?
> CTE는 `WITH` 키워드를 사용하여 정의하는, 쿼리 내에서만 존재하는 임시 명명된 결과 집합입니다. 복잡한 쿼리를 여러 논리적 단계로 나누어 가독성을 높이고, 재귀적인 쿼리를 작성할 때 매우 유용합니다.

**기본 CTE 예제:**
```sql
-- 'Engineering' 부서에 속한 직원들의 정보만 임시 테이블(cte_eng_employees)로 정의하여 사용
WITH cte_eng_employees AS (
    SELECT *
    FROM employees
    WHERE dept_id = (SELECT id FROM departments WHERE dept_name = 'Engineering')
)
-- 정의된 CTE를 사용하여 최종 결과를 조회
SELECT name, email
FROM cte_eng_employees;
```

**재귀 CTE 예제 (계층 구조 데이터 조회):**
```sql
-- 관리자-부하직원 관계를 조회하는 예제
WITH RECURSIVE employee_hierarchy AS (
    -- 시작점(Anchor): 최상위 관리자(manager_id가 NULL인 직원)
    SELECT id, name, manager_id, 1 as level
    FROM employees_with_manager
    WHERE manager_id IS NULL

    UNION ALL

    -- 재귀 부분(Recursive Part): 계층을 하나씩 내려가며 조회
    SELECT e.id, e.name, e.manager_id, eh.level + 1
    FROM employees_with_manager e
    INNER JOIN employee_hierarchy eh ON e.manager_id = eh.id
)
SELECT * FROM employee_hierarchy;
```

---
> [[00. 포스트그레스 목차|⬆️ 목차로 돌아가기]]
> [[3. 기본 SQL/README|⬅️ 이전: 3. 기본 SQL]] | [[5. 데이터 타입/README|➡️ 다음: 5. 데이터 타입]]