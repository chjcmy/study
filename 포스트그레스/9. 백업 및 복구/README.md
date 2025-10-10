# 9. 백업 및 복구
#PostgreSQL/Backup #PostgreSQL/Recovery

데이터는 모든 서비스의 가장 중요한 자산입니다. 하드웨어 장애, 소프트웨어 버그, 혹은 사람의 실수로 인해 데이터가 손실되는 상황에 대비하여 안정적인 백업 및 복구 전략을 수립하는 것은 매우 중요합니다. PostgreSQL은 이를 위한 강력하고 신뢰성 있는 도구들을 제공합니다.

## 9.1. 논리적 백업: `pg_dump` & `pg_restore`

> [!INFO]
> 논리적 백업은 데이터베이스의 스키마(테이블, 뷰 등)와 데이터를 SQL 명령어 또는 아카이브 파일 형태로 저장하는 방식입니다.

### `pg_dump`

`pg_dump`는 특정 데이터베이스를 일관된 상태로 백업하는 표준 유틸리티입니다. 데이터베이스가 운영 중인 상태에서도 다른 사용자의 작업을 막지 않고 안전하게 백업을 수행할 수 있습니다.

**주요 옵션 및 사용법:**

-   **Plain-Text (SQL) 형식**:
    -   백업 결과가 `CREATE TABLE`, `INSERT` 등의 SQL 명령어로 구성된 `.sql` 파일로 생성됩니다.
    -   내용을 직접 확인하고 수정하기 용이하지만, 용량이 크고 복구 시 유연성이 떨어집니다.
    -   복구는 `psql`을 통해 수행합니다.
    ```bash
    # my_db 데이터베이스를 my_db_backup.sql 파일로 백업
    pg_dump -U postgres -d my_db > my_db_backup.sql
    ```

-   **Custom 형식 (`-Fc`)**:
    > [!TIP] 가장 권장되는 방식
    > `pg_dump`의 전용 압축 바이너리 형식(`.dump`)으로 백업합니다. 용량이 작고, `pg_restore`를 통해 병렬 복원, 특정 테이블만 복원하는 등 유연한 복구 작업을 수행할 수 있습니다.
    ```bash
    # my_db 데이터베이스를 custom 형식으로 백업
    pg_dump -U postgres -d my_db -Fc > my_db_backup.dump
    ```

### `pg_restore`

`pg_restore`는 `pg_dump`의 custom, directory, tar 형식으로 생성된 아카이브 파일을 사용하여 데이터베이스를 복원하는 유틸리티입니다.

```bash
# 복원에 앞서 새로운 데이터베이스를 먼저 생성
createdb -U postgres new_db

# my_db_backup.dump 파일을 new_db 데이터베이스로 복원 (-j 옵션으로 병렬 처리)
pg_restore -U postgres -d new_db -j 4 my_db_backup.dump
```

## 9.2. 물리적 백업과 PITR (Point-in-Time Recovery)

논리적 백업만으로는 대용량 데이터베이스를 백업하고 복구하는 데 시간이 너무 오래 걸리거나, 특정 시점으로 정확하게 되돌리기 어렵습니다. PITR은 이러한 한계를 극복하고 데이터 손실을 최소화하는 고급 복구 전략입니다.

### 개념

> [!INFO] PITR (Point-in-Time Recovery)이란?
> PITR은 '특정 시점 복구'를 의미하며, 그 원리는 다음과 같습니다.
> 1.  **베이스 백업(Base Backup)**: 특정 시점의 데이터베이스 클러스터 전체(데이터 파일들)를 물리적으로 복사합니다.
> 2.  **WAL 아카이빙(Archiving)**: 베이스 백업 이후 발생하는 모든 데이터 변경 기록(WAL, Write-Ahead Log)을 별도의 저장소에 차곡차곡 쌓아둡니다.
> 3.  **복구**: 문제가 발생했을 때, 가장 최근의 **베이스 백업**을 복원한 뒤, 그 시점부터 원하는 특정 시간까지 **아카이빙된 WAL 파일**들을 순서대로 재현(replay)하여 데이터베이스를 원하는 특정 시점의 상태로 완벽하게 되돌립니다.

### 장점

-   **최소한의 데이터 손실**: 장애 발생 직전의 특정 초 단위까지 데이터를 복구할 수 있습니다.
-   **빠른 복구**: 전체 백업을 자주 받을 필요 없이, 베이스 백업과 WAL 파일만으로 복구가 가능합니다.
-   **운영 유연성**: 실수로 데이터를 삭제(`DROP TABLE`)했거나 잘못된 데이터를 `UPDATE` 했을 때, 해당 작업이 일어나기 직전의 시간으로 데이터베이스를 되돌릴 수 있습니다.

> [!IMPORTANT] 프로덕션 환경의 표준 재해 복구 전략
> PITR은 프로덕션 환경의 데이터베이스를 위한 사실상의 표준 재해 복구(Disaster Recovery) 전략으로, 모든 중요한 데이터베이스는 PITR 환경을 구축하여 운영해야 합니다.

### 주요 도구 및 설정

-   **`pg_basebackup`**: PITR을 위한 베이스 백업을 생성하는 표준 유틸리티입니다.
-   **WAL 아카이빙 설정**: `postgresql.conf` 파일에서 `archive_mode = on`으로 설정하고, WAL 파일을 저장할 `archive_command`를 지정해야 합니다.
-   **복구 설정**: 복원 시 `recovery.conf` (최신 버전에서는 `postgresql.conf`에 통합) 파일에 어느 시점까지 복구할지(`recovery_target_time`) 등을 지정합니다.

---
> [[00. 포스트그레스 목차|⬆️ 목차로 돌아가기]]
> [[8. 성능 튜닝/README|⬅️ 이전: 8. 성능 튜닝]] | [[10. 사용자 및 권한 관리/README|➡️ 다음: 10. 사용자 및 권한 관리]]