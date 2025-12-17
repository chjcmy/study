# Spring Bean Scope
#Spring/Bean

## Scope
	Bean의 생명주기와 가시성을 결정하는 설정
#### Singleton Scope
	Spring 컨테이너가 초기화될 때 한 번만 Bean을 생성하며, 이후에는 동일한 인스턴슥 재사용된다. (기본)
#### Prototype Scope
	Prototype Scope를 가진 Bean을 조회하면 스프링 컨테이너는 항상 새로운 인스턴스를 생성해서 반환한다.
#### Request Scope 
	웹 요청이 들어오고 나서 응답이 나갈 때까지만 유지되는 Scope이다
#### Session Scope
	웹 세션이 생성되고 종료될때까지 유지되는 Scope이다.
#### Application Scope
	웹의 서블릿 컨텍스트와 같은 범위로 유지되는 Scope이다.

---
> [[00. 스프링 목차.md|⬆️ 목차로 돌아가기]]
> [[Bean 개념.md|⬅️ 이전: Bean 개념]] | [[@Component.md|➡️ 다음: @Component]]