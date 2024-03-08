package test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class TestMain {

	public static void main(String[] args) {
    // TODO fudong 2024/3/8 15:42 此处测试容器的启动
    ApplicationContext acx = new AnnotationConfigApplicationContext(TestConfig.class);
		Object name = acx.getBean("isSuccess");
		System.out.println(name);
	}
}