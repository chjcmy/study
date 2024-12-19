## 정의

`@Target`은 Java 어노테이션이 적용될 수 있는 요소의 종류를 지정하는 메타 어노테이션입니다.

## 패키지

`java.lang.annotation.Target`

## 사용 방법

~~~java

@Target(ElementType.METHOD) 
public @interface MyAnnotation {     // 어노테이션 정의 
}
~~~

## 주요 속성

- `value()`: `ElementType` 열거형의 배열

## ElementType 열거형 값

- `TYPE`: 클래스, 인터페이스, 열거형
- `FIELD`: 필드
- `METHOD`: 메소드
- `PARAMETER`: 메소드 파라미터
- `CONSTRUCTOR`: 생성자
- `LOCAL_VARIABLE`: 지역 변수
- `ANNOTATION_TYPE`: 어노테이션 타입
- `PACKAGE`: 패키지
- `TYPE_PARAMETER`: 타입 파라미터 (Java 8+)
- `TYPE_USE`: 타입 사용 (Java 8+)
- `MODULE`: 모듈 (Java 9+)

## 특징

- 여러 `ElementType`을 동시에 지정 가능
- 지정하지 않으면 모든 요소에 적용 가능

## 예시

~~~java

@Target({ElementType.METHOD, ElementType.FIELD}) 
public @interface MyAnnotation {     
	String value() default ""; 
	}
~~~

## 주의사항

- 적절한 대상을 지정하지 않으면 컴파일 에러 발생
- 런타임에 영향을 주지 않음 (컴파일 시점에만 검사)