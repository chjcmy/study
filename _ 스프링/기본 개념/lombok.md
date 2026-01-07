# Lombok (롬복)
#Java/Lombok #Spring/Tool

> [!INFO] Lombok이란?
> 롬복(Lombok)은 자바(Java) 프로젝트에서 `@Getter`, `@Setter`, `@Builder` 같은 어노테이션(Annotation) 하나만으로 반복적으로 작성해야 하는 상용구 코드(Boilerplate Code)를 컴파일 시점에 자동으로 생성해주는 라이브러리입니다. 코드를 매우 깔끔하고 가독성 높게 만들어주어 거의 모든 최신 자바 프로젝트에서 필수적으로 사용됩니다.

## 주요 어노테이션

### 1. 기본 어노테이션

-   `@Getter` / `@Setter`
    -   클래스의 모든 필드에 대한 `getter`와 `setter` 메소드를 자동으로 생성합니다.
    -   필드나 클래스 레벨에 선언할 수 있습니다.
-   `@ToString`
    -   `toString()` 메소드를 자동으로 오버라이드합니다.
    -   `exclude` 속성을 사용하여 특정 필드를 출력에서 제외할 수 있습니다.
-   `@EqualsAndHashCode`
    -   `equals()`와 `hashCode()` 메소드를 자동으로 오버라이드합니다.

### 2. 생성자 어노테이션

-   `@NoArgsConstructor`
    -   파라미터가 없는 기본 생성자를 생성합니다.
-   `@AllArgsConstructor`
    -   클래스의 모든 필드를 파라미터로 받는 생성자를 생성합니다.
-   `@RequiredArgsConstructor`
    -   `final` 또는 `@NonNull`이 붙은 필드만을 파라미터로 받는 생성자를 생성합니다. 주로 의존성 주입(DI)에 사용됩니다.

### 3. `@Builder`: 빌더 패턴

-   객체 생성을 위한 빌더(Builder) 패턴 코드를 자동으로 생성합니다.
-   생성자에 비해 어떤 값을 어떤 필드에 설정하는지 명확하게 알 수 있고, `setter`를 열어두지 않아도 되어 객체의 불변성(Immutability)을 유지하는 데 도움이 됩니다.

```java
@Builder
@AllArgsConstructor // Builder는 전체 필드를 받는 생성자가 필요합니다.
public class Member {
    private String name;
    private int age;
}

// 사용 예시
Member member = Member.builder()
                    .name("홍길동")
                    .age(30)
                    .build();
```

> [!WARNING] `@Builder`와 필드 초기화
> 기존에 작성하신 내용처럼, 클래스 필드에 직접 초기화 표현식을 사용하는 것(`private List<String> hobbies = new ArrayList<>();`)은 `@Builder`에 의해 무시될 수 있습니다.
> 이 경우, 빌더에 기본값을 설정하려면 `@Builder.Default` 어노테이션을 함께 사용해야 합니다.
> ```java
> @Builder.Default
> private List<String> hobbies = new ArrayList<>();
> ```

### 4. `@Data`: 만능 어노테이션 (주의 필요)

-   `@Getter`, `@Setter`, `@ToString`, `@EqualsAndHashCode`, `@RequiredArgsConstructor`를 **모두 합쳐놓은 강력한 어노테이션**입니다.
-   매우 편리하지만, 의도치 않게 `setter`가 열리는 등 원하지 않는 동작을 유발할 수 있습니다.

> [!CAUTION] `@Data` 사용 시 주의점
> `@Data`는 편리하지만, 특히 엔티티(Entity) 클래스에는 사용하지 않는 것이 좋습니다. 엔티티의 `setter`를 무분별하게 열어두면 객체의 일관성이 깨지기 쉽고, 양방향 연관관계 시 `@ToString`이 순환 참조를 일으켜 스택 오버플로우가 발생할 수 있기 때문입니다.
> **가급적 `@Getter`, `@ToString` 등 필요한 어노테이션만 명시적으로 사용하는 것을 권장합니다.**

---
> [[00. 스프링 목차.md|⬆️ 목차로 돌아가기]]