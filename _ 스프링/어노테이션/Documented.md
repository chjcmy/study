## 정의

`@Documented`는 어노테이션이 Javadoc에 포함되어야 함을 나타내는 메타 어노테이션입니다.

## 패키지

`java.lang.annotation.Documented`

## 사용 방법

~~~java

@Documented 
public @interface MyAnnotation {     
	// 어노테이션 정의 
	}
~~~
## 특징

- 어노테이션에 대한 설명이 Javadoc에 포함됨
- 값을 지정할 필요 없는 마커 어노테이션

## 주요 용도

- API 문서화 시 중요한 어노테이션 정보 포함
- 프레임워크나 라이브러리의 공개 API에 주로 사용

## 예시
~~~java

@Documented @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE) public @interface MyDocumentedAnnotation {     
	String value() default ""; 
	}
~~~
## 영향

- 어노테이션이 적용된 요소의 Javadoc에 해당 어노테이션 정보가 포함됨
- 코드 사용자에게 중요한 메타데이터 제공

## 주의사항

- 내부 구현 세부사항에는 불필요할 수 있음
- Javadoc 생성 시에만 영향을 미침