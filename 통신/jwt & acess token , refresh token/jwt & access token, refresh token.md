# JWT & Token Based Authentication

## JWT (JSON Web Token)
### 개념
- JSON 객체를 사용하여 정보를 안전하게 전송하기 위한 표준 형식
- 디지털 서명이 되어 있어 신뢰성 보장
- Base64로 인코딩된 세 부분으로 구성: Header, Payload, Signature

### 구조
```
header.payload.signature
```
- Header: 토큰 타입과 사용된 알고리즘 정보
- Payload: 실제 전달하려는 데이터 (claims)
- Signature: 토큰의 유효성을 검증하기 위한 서명

## Access Token & Refresh Token
### Access Token
- 실제 API 요청 시 사용되는 인증 토큰
- JWT 형식으로 구현
- 짧은 유효기간 (15분~1시간)
- 모든 보호된 API 요청에 포함

### Refresh Token
- 새로운 Access Token을 발급받기 위한 토큰
- JWT 형식으로 구현
- 긴 유효기간 (7일 이상)
- 서버의 DB/Cache에 저장하여 관리

## 인증 프로세스
1. 초기 로그인
   - 사용자 로그인 성공
   - Access Token + Refresh Token 발급
   - 클라이언트에 두 토큰 전달

2. API 요청
   - Access Token을 Authorization 헤더에 포함
   - 서버는 토큰 유효성 검증
   - 유효한 경우 요청 처리

3. Token 갱신
   - Access Token 만료 시
   - Refresh Token으로 새로운 Access Token 요청
   - 새로운 Access Token 발급

4. 재로그인
   - Refresh Token 만료 시
   - 사용자는 다시 로그인 필요

## 보안 고려사항
### 저장
- Access Token: 메모리 또는 짧은 기간 저장
- Refresh Token: 안전한 저장소 (KeyChain, SecureStorage)

### Token Rotation
- Access Token 갱신 시 Refresh Token도 함께 갱신
- 토큰 탈취에 대한 보안 강화

### 토큰 관리
- Refresh Token Blacklist 관리
- 토큰 만료 시간 적절히 설정
- 보안 이벤트 발생 시 토큰 즉시 무효화

## 장점
1. Stateless Authentication
2. 확장성이 좋음
3. Cross-Origin Resource Sharing (CORS) 지원
4. 모바일 애플리케이션 지원이 용이

## 단점
1. Payload 크기 증가
2. 토큰 저장 관리 필요
3. 한번 발급된 토큰은 만료 전까지 취소 어려움