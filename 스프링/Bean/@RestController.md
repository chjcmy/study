`@Controller` 와 `@ResponseBody` 의 기능을 결합하여 하나의 어노테이션으로 사용할 수 있도록 한다.

- **`@Controller`:** 스프링 MVC 프레임워크에서 컨트롤러로 인식되게 합니다. 요청을 처리하고 뷰를 반환하는 역할을 합니다.
- **`@ResponseBody`:** 컨트롤러 메서드가 반환하는 객체를 HTTP 응답 본문에 직접 포함시킵니다. 즉, 뷰를 찾거나 렌더링하지 않고 데이터 자체를 JSON 또는 XML 형식으로 반환합니다.

`@AliasFor` 는 특정 어노테이션의 속성을 다른 이름으로 사용할 수 있도록 허용

**`@AliasFor(annotation = Controller.class)`:** 이게 있다면 이름이 다르더라도 컨트롤러 역할을 한다