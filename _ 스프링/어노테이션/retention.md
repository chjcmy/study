## 정의

`@Retention`은 어노테이션의 유지 기간을 지정하는 메타 어노테이션입니다.

## 패키지

`java.lang.annotation.Retention`

## 사용 방법

~~~java

@Retention(RetentionPolicy.RUNTIME) 
public @interface MyAnnotation {     // 어노테이션 정의 
	}
~~~
## 주요 속성

- `value()`: `RetentionPolicy` 열거형 값

## RetentionPolicy 열거형 값

1. `SOURCE`: 소스 코드에만 존재, 컴파일 시 제거됨
2. `CLASS`: 클래스 파일에 존재, 런타임 시 제거됨 (기본값)
3. `RUNTIME`: 클래스 파일에 존재, 런타임에도 유지됨

## 특징

- 하나의 `RetentionPolicy`만 지정 가능
- 지정하지 않으면 기본값으로 `CLASS` 적용

## 사용 예시

~~~java

@Retention(RetentionPolicy.RUNTIME) 
public @interface RuntimeAnnotation {     
	String value() default ""; 
	}
~~~

## 주요 용도

- `SOURCE`: 컴파일 시점에만 필요한 정보 (예: 롬복 @Getter)
- `CLASS`: 클래스 로딩 시 사용되는 정보
- `RUNTIME`: 리플렉션을 통해 런타임에 사용되는 정보

## 주의사항

- 필요 이상의 유지 기간 설정은 성능에 영향을 줄 수 있음
- 프레임워크나 라이브러리 사용 시 권장되는 정책 확인 필요