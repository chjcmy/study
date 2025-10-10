# [HackerRank] The PADS
#Database/SQL/ProblemSolving

> [!NOTE] 문제
> `OCCUPATIONS` 테이블을 사용하여 두 가지 다른 형태의 출력을 생성하는 문제입니다.
> 1. 각 이름 뒤에 직업의 첫 글자를 괄호 안에 붙여 출력. (예: `Ashely(P)`)
> 2. 각 직업별 인원수를 세어 특정 문장 형태로 출력. (예: `There are a total of 3 doctors.`)

## 최종 쿼리

### 쿼리 1: 이름과 직업 약어 출력

```sql
SELECT CONCAT(name, '(', LEFT(occupation, 1), ')')
FROM OCCUPATIONS
ORDER BY name ASC;
```

### 쿼리 2: 직업별 인원수 문장 출력

```sql
SELECT CONCAT('There are a total of ', COUNT(occupation), ' ', LCASE(occupation), 's.')
FROM OCCUPATIONS
GROUP BY occupation
ORDER BY COUNT(occupation), occupation ASC;
```

## 핵심 함수

-   **`CONCAT(string1, string2, ...)`**: 여러 문자열을 하나로 합칩니다.
-   **`LEFT(string, number_of_chars)`**: 문자열의 왼쪽에서부터 지정된 개수만큼의 문자를 반환합니다.
-   **`LCASE(string)`** 또는 **`LOWER(string)`**: 문자열을 소문자로 변환합니다.
-   **`COUNT(column)`**: 특정 컬럼의 행 개수를 셉니다.
-   **`GROUP BY`**: 특정 컬럼을 기준으로 데이터를 그룹화하여 집계 함수(COUNT, SUM 등)를 적용합니다.

---
> [[00. 데이터베이스 목차.md|⬆️ 목차로 돌아가기]]