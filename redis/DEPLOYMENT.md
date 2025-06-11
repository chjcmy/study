# Redis 운영 배포 전략

## 메모리 관리

- maxmemory-policy 설정 옵션
- LRU/랜덤/TTL 기반 삭제

## 모니터링

- redis-cli --stat 사용법
- INFO 명령어 출력 해석

## 고가용성

- 센티널을 이용한 페일오버
- 클러스터 자동 재조정

## 클라우드 배포

- AWS ElastiCache 최적화
- Persistence 설정 권장사항
