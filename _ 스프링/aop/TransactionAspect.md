## 주요 특징

- 트랜잭션 경계 설정: 메소드 시작과 종료 시점에 트랜잭션을 자동으로 시작하고 종료합니다.
- 트랜잭션 속성 설정: @Transactional 어노테이션의 속성을 통해 isolation, propagation 등을 설정할 수 있습니다.
- 예외 처리: RuntimeException 발생 시 자동으로 롤백하며, 필요에 따라 rollbackFor 속성으로 커스터마이징 가능합니다.
- 트랜잭션 로깅: AOP를 활용하여 트랜잭션 실행 정보를 자동으로 로깅할 수 있습니다.

## TransactionAspect의 이점

- 코드의 재사용성 향상: 트랜잭션 관리 로직을 중앙화하여 여러 곳에서 재사용할 수 있습니다.
- 유지보수성 향상: 트랜잭션 관리 코드를 비즈니스 로직과 분리하여 유지보수가 용이합니다.
- 코드의 가독성 향상: 비즈니스 로직에서 트랜잭션 관리 코드가 제거되어 코드가 더 깔끔해집니다.

## 구성요소

- Advice: @Transactional 어노테이션을 처리하는 TransactionInterceptor
- Pointcut: @Transactional 어노테이션이 적용된 메소드나 클래스를 대상으로 합니다.
- Advisor: TransactionAttributeSourceAdvisor가 Advice와 Pointcut을 연결합니다.

## 구현 방법

1. @EnableTransactionManagement 어노테이션을 설정 클래스에 추가합니다.
2. PlatformTransactionManager 빈을 설정합니다.
3. @Transactional 어노테이션을 트랜잭션이 필요한 메소드나 클래스에 적용합니다.

예시 코드:

~~~java

@Configuration @EnableTransactionManagement 
public class TransactionConfig {     
	
	@Bean    
	public PlatformTransactionManager transactionManager() {        
		return new DataSourceTransactionManager(dataSource());    
		} 
} 
	
@Service public class UserService {     
	@Transactional    
	public void createUser(User user) {
	        // 사용자 생성 로직    
	        } 
}
~~~
