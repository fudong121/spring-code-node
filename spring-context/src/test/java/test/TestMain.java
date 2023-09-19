package test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class TestMain {

	public static void main(String[] args) {
		ApplicationContext acx = new AnnotationConfigApplicationContext(TestConfig.class);
		Object name = acx.getBean("isSuccess");
		System.out.println(name);
	}
}