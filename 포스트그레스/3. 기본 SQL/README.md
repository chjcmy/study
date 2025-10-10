# 3. 기본 SQL (Basic SQL)

SQL(Structured Query Language)은 데이터베이스와 소통하기 위한 표준 언어입니다. 이 단원에서는 데이터베이스의 구조를 정의하는 **DDL**, 데이터를 조작하는 **DML**, 데이터를 조회하는 **DQL**의 기본 명령어들을 알아봅니다.

---

## 3.1. 데이터 정의어 (DDL - Data Definition Language)

DDL은 데이터베이스의 스키마(구조)를 정의하거나 수정, 삭제하는 데 사용됩니다.

### `CREATE TABLE`: 테이블 생성

데이터를 저장할 '표'를 만듭니다. 각 열(Column)의 이름과 데이터 타입을 지정해야 합니다.

```sql
-- 'employees' 라는 이름의 직원 정보 테이블 생성
CREATE TABLE employees (
    employee_id SERIAL PRIMARY KEY,  -- 직원 ID (자동으로 1씩 증가하는 정수, 기본 키)
    first_name VARCHAR(50),          -- 이름 (최대 50자의 문자열)
    last_name VARCHAR(50),           -- 성 (최대 50자의 문자열)
    email VARCHAR(100) UNIQUE,       -- 이메일 (중복될 수 없음)
    hire_date DATE,                  -- 입사일
    salary NUMERIC(10, 2)            -- 급여 (총 10자리, 소수점 이하 2자리)
);
```
- `SERIAL`: 새로운 행이 추가될 때마다 자동으로 1씩 증가하는 정수 타입입니다.
- `PRIMARY KEY`: 각 행을 고유하게 식별하는 기본 키로 지정합니다.
- `VARCHAR(n)`: 최대 n글자까지 저장할 수 있는 가변 길이 문자열입니다.
- `UNIQUE`: 해당 열의 모든 값은 서로 달라야 함을 의미합니다.
- `DATE`: 날짜 값을 저장합니다.
- `NUMERIC(p, s)`: 총 p자리의 숫자 중 소수점 이하 s자리를 포함하는 정확한 숫자 타입입니다.

---

## 3.2. 데이터 조작어 (DML - Data Manipulation Language)

DML은 테이블의 데이터를 추가, 수정, 삭제하는 데 사용됩니다.

### `INSERT INTO`: 데이터 추가

테이블에 새로운 행(Row)을 추가합니다.

```sql
-- 'employees' 테이블에 새로운 직원 정보 추가
INSERT INTO employees (first_name, last_name, email, hire_date, salary)
VALUES ('Seonghyeon', 'Choe', 'sh.choe@example.com', '2025-10-11', 70000.00);

INSERT INTO employees (first_name, last_name, email, hire_date, salary)
VALUES ('Gildong', 'Hong', 'gd.hong@example.com', '2024-01-15', 85000.00);
```

### `UPDATE`: 데이터 수정

기존 행의 데이터를 수정합니다. `WHERE` 절을 사용해 수정할 대상을 지정해야 합니다.
**주의: `WHERE` 절을 생략하면 테이블의 모든 행이 수정됩니다.**

```sql
-- 직원 ID가 1인 직원의 급여를 75000으로 인상
UPDATE employees
SET salary = 75000.00
WHERE employee_id = 1;
```

### `DELETE FROM`: 데이터 삭제

테이블에서 특정 행을 삭제합니다. `WHERE` 절을 사용해 삭제할 대상을 지정해야 합니다.
**주의: `WHERE` 절을 생략하면 테이블의 모든 행이 삭제됩니다.**

```sql
-- 직원 ID가 2인 직원의 정보를 삭제
DELETE FROM employees
WHERE employee_id = 2;
```

---

## 3.3. 데이터 질의어 (DQL - Data Query Language)

DQL은 테이블에서 원하는 데이터를 조회하고 가져오는 데 사용됩니다.

### `SELECT`: 데이터 조회

가장 기본적이고 중요한 SQL 명령어입니다.

```sql
-- 'employees' 테이블의 모든 열과 모든 행을 조회
SELECT * FROM employees;

-- 특정 열(이름, 성, 급여)만 조회
SELECT first_name, last_name, salary FROM employees;
```

### `WHERE`: 조건 필터링

`SELECT` 문과 함께 사용되어, 특정 조건을 만족하는 행만 조회합니다.

```sql
-- 급여가 70000 이상인 직원만 조회
SELECT * FROM employees
WHERE salary >= 70000;

-- 성(last_name)이 'Choe'인 직원 조회
SELECT * FROM employees
WHERE last_name = 'Choe';
```

### `ORDER BY`: 결과 정렬

조회된 결과를 특정 열을 기준으로 정렬합니다.

```sql
-- 급여를 기준으로 오름차순(ASC, 기본값) 정렬
SELECT * FROM employees
ORDER BY salary; -- ASC는 생략 가능

-- 급여를 기준으로 내림차순(DESC) 정렬
SELECT * FROM employees
ORDER BY salary DESC;
```

### `GROUP BY` & `HAVING`: 데이터 그룹화

특정 열을 기준으로 데이터를 그룹화하고, 각 그룹에 대한 집계 함수(예: `COUNT`, `AVG`, `SUM`)를 적용할 때 사용합니다. `HAVING`은 `GROUP BY`로 생성된 그룹에 대한 조건을 지정할 때 사용됩니다.

```sql
-- 'departments' 테이블이 있다고 가정하고, 각 부서별 평균 급여 계산
SELECT department_id, AVG(salary)
FROM employees
GROUP BY department_id;

-- 평균 급여가 80000 이상인 부서만 조회
SELECT department_id, AVG(salary)
FROM employees
GROUP BY department_id
HAVING AVG(salary) >= 80000;
```