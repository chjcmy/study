# Spring Boot
#Spring/SpringBoot

> [!INFO] Spring Boot란?
> 스프링 부트(Spring Boot)는 스프링 프레임워크를 더 쉽고 빠르게 사용할 수 있도록 돕는 도구입니다. 복잡한 초기 설정을 대폭 줄여주어, 개발자가 애플리케이션의 비즈니스 로직에만 집중할 수 있는 환경을 제공합니다.

## 스프링 부트의 핵심 특징

### 1. 내장 WAS (Embedded Web Application Server)

-   기존의 스프링 프레임워크는 웹 애플리케이션을 실행하기 위해 톰캣(Tomcat)과 같은 WAS를 별도로 설치하고, 생성된 `war` 파일을 WAS에 배포하는 과정을 거쳐야 했습니다.
-   스프링 부트는 **톰캣, 제티(Jetty), 언더토우(Undertow) 같은 WAS를 내장**하고 있습니다.
-   이로 인해 별도의 WAS 설치 없이, 애플리케이션을 **독립적인 `jar` 파일로 패키징**하여 `java -jar` 명령어 하나만으로 어디서든 간단하게 실행하고 배포할 수 있습니다.

> [!NOTE] JAR vs WAR
> - **JAR (Java Archive)**: 자바 클래스와 관련 리소스들을 하나로 묶은 파일. 내장 WAS 덕분에 `main()` 메소드를 실행하는 것만으로 웹 애플리케이션을 구동할 수 있습니다.
> - **WAR (Web Application Archive)**: 웹 애플리케이션을 위한 파일 형식. 서블릿 컨테이너(WAS)에 배포되어야만 실행될 수 있습니다.

### 2. 자동 설정 (Auto-configuration)

-   스프링 부트의 가장 강력한 기능 중 하나입니다.
-   개발자가 프로젝트의 `classpath`에 추가한 라이브러리(의존성)를 감지하여, 그에 맞춰 **필요한 스프링 빈(Bean)들을 자동으로 구성하고 등록**해 줍니다.
-   예를 들어, `spring-boot-starter-data-jpa` 의존성을 추가하면, 스프링 부트는 데이터베이스 연결(`DataSource`), JPA 관련 설정(`EntityManagerFactory` 등)을 자동으로 구성하려고 시도합니다.
-   이러한 자동 설정 덕분에, 과거 XML이나 Java 클래스로 일일이 작성해야 했던 수많은 설정 코드가 `@SpringBootApplication` 어노테이션 하나로 대체됩니다.

### 3. 스타터 의존성 (Starter Dependencies)

-   특정 기능을 개발하는 데 필요한 **관련 라이브러리들의 묶음**을 '스타터(Starter)'라는 이름으로 제공합니다.
-   예를 들어, 웹 애플리케이션을 개발하고 싶다면 `spring-boot-starter-web` 의존성 하나만 추가하면 됩니다. 그러면 웹 개발에 필요한 톰캣, Spring MVC, Jackson(JSON 라이브러리) 등이 **호환되는 버전으로 한 번에** 포함됩니다.
-   이를 통해 개발자는 라이브러리 버전 충돌 문제를 걱정할 필요 없이 간편하게 의존성을 관리할 수 있습니다.

---
> [[00. 스프링 목차.md|⬆️ 목차로 돌아가기]]