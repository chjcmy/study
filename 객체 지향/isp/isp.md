## Interface Segregation Principle

		"클라이언트는 자신이 사용하지 않는 메서드에 의존하면 안된다"
		한 클래스는 자신이 필요하지 않는 인터페이스는 구현하지 말아야 한다는 원칙

	원칙을 지키는 이유로는 클래스 간의 결합도를 낮추고, 변경에 더 유연하게 대응할 수 있다
	예를 들어 여러 기능을 가진 인터페이스를 구현하는 대신, 각 기능을 나타내는 여러 인터페이스를 구현하는 대신, 각 기능을 나타내는 여러 인터페이스를 구현하는 것이 원칙에 부합하다
	이렇게 하면 각 인터페이스는 자신의 책임에만 집중할 수 있으며, 클래스는 필요한 인터페이스만 구현하면 된다

```java
public interface MultiFunctionDevice {
    void print();
    void fax();
    void scan();
}
```

```java
public class MultiFunctionPrinter implements MultiFunctionDevice {
    @Override
    public void print() {
        // print logic
    }

    @Override
    public void fax() {
        // fax logic
    }

    @Override
    public void scan() {
        // scan logic
    }
}
```

하지만 여기에서 fax라는 메서드를 사용하지 않지만 만드는 것은 isp 원칙을 위반 하는 것이다. <br/>
그러기 때문에 여러 인터페이스를 만들고, 클래스는 필요한 인터페이스만 구현하도록 한다.

```java
public interface Printer {
    void print();
}

public interface Fax {
    void fax();
}

public interface Scanner {
    void scan();
}
```

```java
public class SimplePrinter implements Printer {
    @Override
    public void print() {
        // print logic
    }
}

public class AdvancedPrinter implements Printer, Scanner {
    @Override
    public void print() {
        // print logic
    }

    @Override
    public void scan() {
        // scan logic
    }
}
```
