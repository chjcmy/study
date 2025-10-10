# 2. PostgreSQL 설치 및 설정
#PostgreSQL/Setup

이 문서에서는 주요 운영체제(macOS, Windows, Linux)별로 PostgreSQL을 설치하고, 기본적인 사용을 위해 데이터베이스와 사용자를 생성하는 방법을 안내합니다.

## 2.1. macOS에서 설치 (Homebrew 사용)

> [!NOTE] macOS
> macOS에서는 패키지 관리자인 [Homebrew](https://brew.sh/)를 사용하는 것이 가장 간편합니다.

### 1. Homebrew 설치
터미널에 다음 명령어를 붙여넣어 Homebrew를 설치합니다.
```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

### 2. PostgreSQL 설치
Homebrew를 이용해 PostgreSQL을 설치합니다.
```bash
brew install postgresql
```

### 3. PostgreSQL 서비스 시작
설치가 완료되면, 컴퓨터가 켜질 때마다 PostgreSQL이 자동으로 실행되도록 서비스를 등록하고 시작합니다.
```bash
brew services start postgresql
```
- **서비스 중지**: `brew services stop postgresql`
- **서비스 재시작**: `brew services restart postgresql`

### 4. PostgreSQL 접속
`psql` 명령어를 사용해 기본 `postgres` 데이터베이스에 접속할 수 있습니다.
```bash
psql postgres
```
접속 후 `\q`를 입력하면 psql을 종료할 수 있습니다.

## 2.2. Windows에서 설치 (EDB 인스톨러 사용)

> [!NOTE] Windows
> Windows에서는 EnterpriseDB(EDB)에서 제공하는 공식 설치 프로그램을 사용하는 것이 가장 쉽습니다.

1.  **설치 프로그램 다운로드**: [PostgreSQL 공식 다운로드 페이지](https://www.postgresql.org/download/windows/)에서 Windows용 설치 프로그램을 다운로드합니다.
2.  **설치 마법사 실행**:
    - 다운로드한 `.exe` 파일을 실행하고, 설치 경로, 구성 요소(pgAdmin 4 포함) 등을 선택합니다.
    - **슈퍼유저(postgres) 비밀번호 설정**: 데이터베이스 최고 관리자인 `postgres` 사용자의 비밀번호를 설정합니다.
    > [!IMPORTANT] 반드시 기억해야 합니다.
    - **포트(Port)**: 기본값인 `5432`를 사용합니다.
    - **로케일(Locale)**: 'Default locale' 또는 'C'를 선택합니다.
3.  **접속**:
    - **GUI**: 시작 메뉴에서 `pgAdmin 4`를 실행하여 그래픽 인터페이스로 데이터베이스에 접속하고 관리할 수 있습니다.
    - **CLI**: 시작 메뉴에서 `SQL Shell (psql)`을 실행하고, 설치 시 설정한 비밀번호를 입력하여 접속합니다.

## 2.3. Linux (Ubuntu/Debian)에서 설치

> [!NOTE] Linux
> Ubuntu/Debian 계열의 리눅스에서는 `apt` 패키지 관리자를 사용합니다.

1.  **패키지 목록 업데이트 및 설치**:
    ```bash
    sudo apt update
    sudo apt install postgresql postgresql-contrib
    ```
2.  **접속**:
    설치 과정에서 시스템에 `postgres` 사용자가 생성됩니다. 다음 명령어로 `postgres` 사용자로 전환하여 `psql`을 실행합니다.
    ```bash
    sudo -i -u postgres
    psql
    ```

## 2.4. 기본 설정 (사용자 및 데이터베이스 생성)

> [!TIP] 보안 가이드
> 보안을 위해 일반적으로 `postgres` 슈퍼유저를 직접 사용하기보다는, 별도의 사용자와 데이터베이스를 만들어 애플리케이션에 사용합니다.

1.  **슈퍼유저로 접속**: 각 OS에 맞는 방법으로 `postgres` 사용자로 `psql`에 접속합니다.

2.  **사용자(Role) 생성**:
    `CREATE USER` 명령으로 새 사용자를 만들고 비밀번호를 설정합니다.
    ```sql
    CREATE USER myuser WITH ENCRYPTED PASSWORD 'mypassword';
    ```
    - `myuser`: 원하는 사용자 이름으로 변경
    - `mypassword`: 원하는 비밀번호로 변경

3.  **데이터베이스 생성**:
    `CREATE DATABASE` 명령으로 새 데이터베이스를 만들고, 방금 생성한 사용자를 소유자(Owner)로 지정합니다.
    ```sql
    CREATE DATABASE mydatabase OWNER myuser;
    ```
    - `mydatabase`: 원하는 데이터베이스 이름으로 변경
    - `myuser`: 방금 생성한 사용자 이름

4.  **접속 테스트**:
    `psql`을 종료(`\q`)하고, 새로 만든 사용자로 새로 만든 데이터베이스에 접속해 봅니다.
    ```bash
    psql -U myuser -d mydatabase
    ```
    위 명령 실행 후 비밀번호를 묻는 메시지가 나오면 설정한 비밀번호를 입력합니다.

## 2.5. 주요 설정 파일 (참고)

-   `postgresql.conf`: 데이터베이스 서버의 전반적인 설정을 관리하는 파일입니다. (예: `listen_addresses`, `max_connections`)
-   `pg_hba.conf`: 클라이언트의 인증 및 접속 허용 규칙을 설정하는 파일입니다. (HBA: Host-Based Authentication) 원격 접속을 허용하려면 이 파일을 수정해야 합니다.

---
> [[00. 포스트그레스 목차|⬆️ 목차로 돌아가기]]
> [[1. PostgreSQL 소개/README|⬅️ 이전: 1. PostgreSQL 소개]] | [[3. 기본 SQL/README|➡️ 다음: 3. 기본 SQL]]