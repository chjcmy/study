# 10. 사용자 및 권한 관리
#PostgreSQL/Security #PostgreSQL/Admin

데이터베이스 보안의 첫걸음은 각 사용자가 자신의 역할에 꼭 필요한 작업만 수행할 수 있도록 권한을 세밀하게 제어하는 것입니다. PostgreSQL은 '역할(Role)'이라는 유연한 개념을 통해 사용자 및 권한 관리를 수행합니다.

## 10.1. 역할 (Role)

> [!INFO] 역할(Role)이란?
> PostgreSQL에서는 사용자와 그룹을 구분하지 않고 '역할(Role)'이라는 단일한 개념으로 관리합니다. 역할은 데이터베이스에 접속할 수 있는 사용자일 수도 있고, 여러 권한을 묶어놓은 그룹일 수도 있습니다.

-   **사용자 역할**: `LOGIN` 속성을 가진 역할. 데이터베이스에 접속할 수 있습니다.
-   **그룹 역할**: `NOLOGIN` 속성을 가진 역할. 접속은 할 수 없지만, 다른 역할에게 부여하여 권한의 묶음으로 사용됩니다.

### 역할 생성 및 삭제

-   **`CREATE ROLE`**: 새로운 역할을 생성합니다.
-   **`DROP ROLE`**: 기존 역할을 삭제합니다.
-   **`ALTER ROLE`**: 기존 역할의 속성을 변경합니다.

```sql
-- 1. 로그인 가능하고, 암호를 가진 '사용자' 역할 생성
CREATE ROLE web_app WITH LOGIN PASSWORD 'strong_password';

-- 2. 다른 역할에게 부여하기 위한 '그룹' 역할 생성
CREATE ROLE readonly_group;

-- 3. 역할의 속성 변경 (슈퍼유저 권한 부여)
ALTER ROLE web_app SUPERUSER;

-- 4. 역할 삭제
DROP ROLE web_app;
```

## 10.2. 권한 부여 및 회수

역할을 만들었다면, 이제 각 역할이 어떤 데이터베이스 객체(테이블, 스키마, 뷰 등)에 대해 어떤 작업을 수행할 수 있는지 권한(Privilege)을 설정해야 합니다.

### `GRANT`: 권한 부여

`GRANT` 명령어는 특정 역할에게 특정 객체에 대한 권한을 부여합니다.

**주요 권한의 종류:**
- `SELECT`: 데이터를 읽을 권한
- `INSERT`: 데이터를 추가할 권한
- `UPDATE`: 데이터를 수정할 권한
- `DELETE`: 데이터를 삭제할 권한
- `USAGE`: 스키마 또는 시퀀스 등 특정 객체를 사용할 권한
- `CONNECT`: 데이터베이스에 접속할 권한
- `ALL PRIVILEGES`: 모든 권한

```sql
-- 'readonly_group' 역할에게 'employees' 테이블의 SELECT 권한만 부여
GRANT SELECT ON TABLE employees TO readonly_group;

-- 'web_app' 역할에게 'employees' 테이블의 모든 DML(SELECT, INSERT, UPDATE, DELETE) 권한 부여
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE employees TO web_app;

-- 'web_app' 역할에게 'my_db' 데이터베이스에 접속할 수 있는 권한 부여
GRANT CONNECT ON DATABASE my_db TO web_app;
```

### `REVOKE`: 권한 회수

`REVOKE` 명령어는 `GRANT`로 부여했던 권한을 다시 회수합니다.

```sql
-- 'web_app' 역할로부터 'employees' 테이블의 DELETE 권한을 회수
REVOKE DELETE ON TABLE employees FROM web_app;
```

## 10.3. 그룹 멤버십을 통한 권한 관리

`GRANT`를 사용하여 역할(그룹)을 다른 역할(사용자)에게 부여할 수 있습니다. 이를 통해 여러 권한을 개별 사용자에게 일일이 부여하는 대신, 그룹 역할에만 부여하고 사용자를 그룹에 포함시키는 효율적인 관리가 가능합니다.

```sql
-- 1. 'readonly_group'이 가진 모든 권한을 'web_app' 사용자에게 상속
GRANT readonly_group TO web_app;

-- 2. 'web_app' 사용자로부터 'readonly_group' 역할의 멤버십을 회수
REVOKE readonly_group FROM web_app;
```

> [!TIP] 권장되는 권한 관리 패턴
> 1.  **그룹 역할 정의**: `read_only`, `read_write` 등 애플리케이션에 필요한 권한의 종류에 따라 그룹 역할을 만듭니다.
> 2.  **그룹에 권한 부여**: 실제 테이블이나 객체에 대한 권한은 이 그룹 역할들에게만 부여합니다.
> 3.  **사용자 생성 및 그룹 할당**: 실제 데이터베이스 접속이 필요한 사용자 역할을 만들고, 이 사용자에게 미리 정의된 그룹 역할을 부여합니다.
> 
> 이렇게 하면 새로운 사용자가 추가되거나 권한 정책이 변경될 때, 개별 사용자의 권한이 아닌 그룹 역할의 권한만 수정하면 되므로 관리가 매우 용이해집니다.

```sql
-- 예시: 읽기 전용 사용자와 쓰기 가능 사용자 분리
-- 1. 그룹 역할 생성
CREATE ROLE read_only_users;
CREATE ROLE read_write_users;

-- 2. 그룹에 권한 부여
GRANT CONNECT ON DATABASE my_db TO read_only_users, read_write_users;
GRANT USAGE ON SCHEMA public TO read_only_users, read_write_users;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO read_only_users;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO read_write_users;

-- 3. 사용자 생성 및 그룹 할당
CREATE ROLE analyst_user WITH LOGIN PASSWORD 'password123';
CREATE ROLE developer_user WITH LOGIN PASSWORD 'password456';

GRANT read_only_users TO analyst_user;    -- 분석가는 읽기 전용
GRANT read_write_users TO developer_user; -- 개발자는 읽기/쓰기 가능
```

---
> [[00. 포스트그레스 목차|⬆️ 목차로 돌아가기]]
> [[9. 백업 및 복구/README|⬅️ 이전: 9. 백업 및 복구]] | [[11. 벡터와 pgvector 확장/README|➡️ 다음: 11. 벡터와 pgvector 확장]]