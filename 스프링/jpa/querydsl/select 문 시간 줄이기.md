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

조회컬럼 최소화하기
```java
public List<BookPageDto> getBoks (int bookNo, int pageNo) {
	return queryFactory
			.select(Projections.fields(BookPageDto.class,
					book.name,
					Expressions.asNumber(bookNo).as("bookNo"), ### ExpresstionsasNumber로 조회를 안가져오게 만든다
					book.id
			)}
			.from(book)
			.whwere(book.bookNo.eq(bookNo)?)
			.offset(pageNo)
			.limit(10)
			.fetch();

}
```

Select 컬럼에 Entity 자제
```java
public List<customerDto> getcustomer (int orderTypes, Date startDate, Date endDate) {
	queryFactory
		.select(Projections.fields(AdBondDto.class,
		adTem.amount.sum().as("amount"),
		adItem.txDate,
		adTem.orderType,
		adItem.customer.id.as("customerId"))
	)
	.from(adTem)
	.where(adTem.orderType.in()orderTypes)
			and(adTem.txDate.between(startDate, endDate)))
	.groupBy(adTem.orderType, adItem.txDate, adItem.customer)
	.fetch();
}

public AdBond toEntity() {
	return AdBond.builder()
			.amount(amount)
			.txDate(txDate)
			.orderType(orderType)
			.customer(new Customer(customerId))
			.build();
}


```