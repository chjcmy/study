exist 메소드 금지
```java
public Boolean exist(Long bookId) {
	Integer fetchOne = queryFactory
			.selectOne()
			.from(book)
			.where(book.id.eq(bookId))
			.fetchFirst();

		return fetchOne != null;

}
```

크로스 조인 회피
	인너 조인을 사용할것
	
Entity 보다는 Dto 를 우선
	Entity를 사용할경우 불필요한 컬럼 까지 조회하여 OneToOne, n + 1 쿼리등 단순 조회 기능에서는 성능 이슈 요소가 많다.
