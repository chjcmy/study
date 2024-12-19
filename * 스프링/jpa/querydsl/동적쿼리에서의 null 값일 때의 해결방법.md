```java
	@Override
	public List<Academy> findDynamicQueryAdvance(String name, String address, String phoneNumber) {
		return queryFactory
				.selectFrom(academy)
				.where(eqName(name),
					eqAddress(address),
					eqPhoneNumber(phoneNumber))
				.fetch();
	}

private BooleanExpression eqName(String name) {
	if (StringUtils.isEmpty(name)) {
		return null;
	}
	return academy.name.eq(name);
}


### 까먹을때 마다 하나 씩 만들어 볼수 있도록!!!
```

