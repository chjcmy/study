# 5. 데이터 타입 (Data Types)

테이블을 설계할 때 각 열(column)에 어떤 종류의 데이터가 저장될지 정확히 지정하는 것은 매우 중요합니다. PostgreSQL은 다양한 종류의 데이터를 효율적으로 저장하고 처리할 수 있도록 풍부한 데이터 타입을 제공합니다.

---

## 5.1. 숫자 타입 (Numeric Types)

정수, 소수 등 숫자 데이터를 저장합니다.

-   **`INTEGER`** 또는 **`INT`**: 일반적인 범위의 정수를 저장합니다. (4바이트, 약 -21억 ~ 21억)
    -   `SMALLINT`: 더 작은 범위의 정수를 저장할 때 사용합니다. (2바이트, 약 -32,768 ~ 32,767)
    -   `BIGINT`: 매우 큰 범위의 정수를 저장할 때 사용합니다. (8바이트)
-   **`SERIAL`**, **`BIGSERIAL`**: `INTEGER` 또는 `BIGINT`와 동일하지만, 새로운 행이 추가될 때마다 자동으로 1씩 증가하는 값이 입력됩니다. 주로 `PRIMARY KEY`에 사용됩니다.
-   **`NUMERIC(p, s)`** 또는 **`DECIMAL(p, s)`**: 정확한 소수점을 표현할 때 사용합니다. 금융 데이터처럼 정밀한 계산이 필요할 때 필수적입니다.
    -   `p`: 전체 자릿수 (정수부 + 소수부)
    -   `s`: 소수점 이하 자릿수
    -   예: `NUMERIC(10, 2)`는 `12345678.90`과 같은 숫자를 저장할 수 있습니다.
-   **`REAL`**, **`DOUBLE PRECISION`**: 부동 소수점 숫자를 저장합니다. 근사값이므로 정밀한 계산에는 적합하지 않을 수 있습니다.

**예제:**
```sql
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    stock_count INT,
    price NUMERIC(10, 2)
);
```

---

## 5.2. 문자 타입 (Character Types)

텍스트 데이터를 저장합니다.

-   **`VARCHAR(n)`**: 최대 `n`글자까지 저장할 수 있는 가변 길이 문자열입니다. 저장된 데이터의 길이에 맞춰 공간을 사용하므로 효율적입니다.
-   **`CHAR(n)`**: `n`글자로 길이가 고정된 문자열입니다. 만약 `n`보다 짧은 데이터를 저장하면 나머지는 공백으로 채워집니다.
-   **`TEXT`**: 길이 제한이 없는 가변 길이 문자열입니다. 긴 텍스트를 저장할 때 사용합니다. 대부분의 경우 `VARCHAR` без 길이 제한과 유사하게 동작하며, 특별한 이유가 없다면 `VARCHAR`나 `TEXT`를 사용하는 것이 일반적입니다.

**예제:**
```sql
CREATE TABLE members (
    username VARCHAR(20) UNIQUE NOT NULL, -- 사용자 아이디 (최대 20자, 중복 불가, NULL 불가)
    description TEXT                     -- 자기소개 (길이 제한 없음)
);
```

---

## 5.3. 날짜/시간 타입 (Date/Time Types)

날짜와 시간 정보를 저장합니다.

-   **`DATE`**: 날짜만 저장합니다. (예: `2025-10-11`)
-   **`TIME`**: 시간만 저장합니다. (예: `14:30:00`)
-   **`TIMESTAMP`**: 날짜와 시간을 함께 저장합니다. (예: `2025-10-11 14:30:00`)
-   **`TIMESTAMPTZ`** (`TIMESTAMP WITH TIME ZONE`): `TIMESTAMP`와 동일하지만, 타임존 정보까지 함께 저장하고 관리합니다. 여러 국가의 사용자를 대상으로 하는 서비스에서 표준 시간을 관리할 때 매우 중요합니다. 저장 시 UTC(협정 세계시)로 변환되어 저장되고, 조회 시 클라이언트의 타임존에 맞춰 보여줍니다.

**예제:**
```sql
CREATE TABLE posts (
    id SERIAL PRIMARY KEY,
    content TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW() -- 게시글 작성 시간 (기본값으로 현재 시간 입력)
);
```
- `NOW()`: 현재 날짜와 시간을 반환하는 PostgreSQL 내장 함수입니다.

---

## 5.4. 논리 타입 (Boolean Type)

참(True), 거짓(False), 또는 알 수 없음(NULL) 세 가지 상태를 저장합니다.

-   **`BOOLEAN`**: `true` 또는 `false` 값을 가집니다. PostgreSQL은 `t`, `yes`, `1`을 `true`로, `f`, `no`, `0`을 `false`로 인식하는 등 유연한 입력이 가능합니다.

**예제:**
```sql
CREATE TABLE settings (
    user_id INT,
    receive_email BOOLEAN DEFAULT true -- 이메일 수신 여부 (기본값은 true)
);
```

---

## 5.5. JSON 타입

JSON(JavaScript Object Notation) 형식의 데이터를 그대로 저장할 수 있습니다. 스키마가 유동적인 데이터를 다룰 때 매우 유용합니다.

-   **`JSON`**: 입력된 JSON 텍스트를 그대로 저장합니다. JSON 형식이 올바른지만 검사합니다.
-   **`JSONB`**: JSON 데이터를 내부적으로 최적화된 이진(Binary) 형태로 변환하여 저장합니다.
    -   **장점**: `JSON` 타입보다 처리 속도가 훨씬 빠르고, 인덱싱을 지원하여 특정 JSON 필드를 기준으로 데이터를 검색하는 성능이 뛰어납니다.
    -   **단점**: 데이터를 저장할 때 이진 변환 과정 때문에 입력 속도가 `JSON` 타입보다 약간 느립니다.

**특별한 이유가 없다면, 대부분의 경우 `JSONB` 타입을 사용하는 것이 권장됩니다.**

**예제:**
```sql
CREATE TABLE events (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    properties JSONB  -- 이벤트의 상세 속성 (구조가 유동적일 수 있음)
);

-- JSONB 데이터 추가 및 조회
INSERT INTO events (name, properties)
VALUES ('user_login', '{"browser": "Chrome", "os": "macOS", "login_time": "2025-10-11T10:00:00Z"}');

-- JSON 내부의 특정 필드로 데이터 검색 (->> 연산자 사용)
SELECT * FROM events
WHERE properties ->> 'os' = 'macOS';
```