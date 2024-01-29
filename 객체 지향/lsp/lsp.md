		서브 타입은 그것의 기반 타입으로 대체될 수 있어야 한다
		클래스 상속 또는 인터페이스 구현 관점에서 볼 때, 자식 클래스 또는 구현 클래스가 부모 클래스 또는 인터페이스의 역할을 완전히 대체할 수 있어야 함을 의미한다

	Java에서 인터펭스는 LSP를 지원 하는 한지 방법 이다
	인터페이스를 구현하는 모든 클래스는 해당 인터페이스의 모든 메소들를 구현해야 하므로, 인터페이스 타입의 변수는 그 인터페이스를 구현하는 어떤 클래스의 객체로도 대체 될 수 있다

```java
public interface Animal {
    void makeSound();
}

public class Dog implements Animal {
    @Override
    public void makeSound() {
        System.out.println("Bark!");
    }
}

public class Cat implements Animal {
    @Override
    public void makeSound() {
        System.out.println("Meow!");
    }
}
```

		이 경우, Anmal 인터페이스를 구현하는 Dog 클래스와 Cat 클래스는 Animal 타입의 변수로 대체도리 수 있다

```java
Animal myPet = new Dog();
myPet.makeSound();  // Outputs "Bark!"

myPet = new Cat();
myPet.makeSound();  // Outputs "Meow!"
```

		딸서, 인터페이스는 Liskov Substitution Principle을 지원하는 한 방법이지만, 이 원칙은 클래스 상속에도 적용된다.
		중요한 것은 서브타입(자식 클래스 또는 구현 클래스)이 기반 타입(부모 클래스 또는 인터페이스)의 역할을 완전히 대체할 수 있어야 한다는 것이다.