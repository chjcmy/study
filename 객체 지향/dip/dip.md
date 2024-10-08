## Dependency Inversion Principle

		"고수준 모듈은 저수준 모듈에 의존하면 안되며, 둘 다 추상화에 의존해야한다"
		구체적인 클래스에 의존하는 대신 인터페이스나 추상 클래스에 의존해야한다

* 이 원칙을 지키는 이유
	* 결합도를 낮출수 있다
	* 변경에 더 유연하게 대응할 수 있다

	예를 들어, 데이터베이스 접근 로직을 구현하는 클래스가 있을 때, 이 클래스를 직접 사용하는 대신 인터페이스를 통해 접근하면, 데이터베이스 접근 방식이 변경 되더라도 사용하는 측의 코드는 변경할 필요가 없게 된다
	
```java
public interface DatabaseAccess {
    void saveData(String data);
}

public class MySQLDatabaseAccess implements DatabaseAccess {
    @Override
    public void saveData(String data) {
        // MySQL database access logic
    }
}

public class DataProcessor {
    private final DatabaseAccess databaseAccess;

    public DataProcessor(DatabaseAccess databaseAccess) {
        this.databaseAccess = databaseAccess;
    }

    public void processData(String data) {
        // process data
        databaseAccess.saveData(data);
    }
}
```
