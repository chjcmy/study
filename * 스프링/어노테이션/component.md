## 정의

`@Component`는 Spring Framework에서 클래스를 자동으로 빈(Bean)으로 등록하도록 지시하는 어노테이션입니다.

## 패키지

`org.springframework.stereotype.Component`

## 사용 방법

~~~java

@Component 
public class MyComponent {     
	// 클래스 정의 
	}
~~~

## 주요 특징

- 스프링의 컴포넌트 스캔 메커니즘에 의해 자동으로 감지되고 등록됨
- 싱글톤 스코프로 기본 생성됨
- 다른 스테레오타입 어노테이션(@Service, @Repository, @Controller 등)의 기본이 되는 어노테이션

## 속성

- `value`: 빈의 이름을 지정 (선택사항)java
    
    ~~~java
    @Component("myCustomBean") 
    public class MyComponent { }
    ~~~

## 주요 용도

- 비즈니스 로직을 포함하는 일반적인 스프링 빈 정의
- 다른 스테레오타입 어노테이션의 기반 제공
- 자동 의존성 주입(Autowiring)의 대상이 됨

## 관련 어노테이션

- @Service: 비즈니스 로직 계층
- @Repository: 데이터 접근 계층
- @Controller: 프레젠테이션 계층 (웹 요청 처리)

## 주의사항

- 컴포넌트 스캔이 활성화되어 있어야 자동 감지됨
- 너무 많은 @Component 사용은 애플리케이션 구조를 모호하게 만들 수 있음
- 필요에 따라 더 구체적인 스테레오타입 어노테이션 사용 권장

## 예시

~~~java

@Component 
public class EmailService {     
	public void sendEmail(String to, String subject, String body) {
	        // 이메일 전송 로직    
	        } 
	    }
~~~