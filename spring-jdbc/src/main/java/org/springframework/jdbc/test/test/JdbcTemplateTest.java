package org.springframework.jdbc.test.test;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.test.enity.Book;
import org.springframework.jdbc.test.service.BookService;

/**
 * @Author m.kong
 * @Date 2021/2/28 下午9:41
 * @Version 1.0
 */
public class JdbcTemplateTest {


  public static void main(String[] args) {
    ApplicationContext context = new ClassPathXmlApplicationContext("bean1.xml");
    BookService bookService = context.getBean("bookService", BookService.class);
    Book book = new Book();
    book.setUserId(1);
    book.setUsername("Micah");
    book.setUserStatus("online");
    bookService.addBook(book);
  }

  /**
   * 单增
   */

  public void testAdd() {
    ApplicationContext context = new ClassPathXmlApplicationContext("bean1.xml");
    BookService bookService = context.getBean("bookService", BookService.class);
    Book book = new Book();
    book.setUserId(1);
    book.setUsername("Micah");
    book.setUserStatus("online");
    bookService.addBook(book);
  }


  public void testUpdate() {
    ApplicationContext context = new ClassPathXmlApplicationContext("bean1.xml");
    BookService bookService = context.getBean("bookService", BookService.class);
    Book book = new Book();
    book.setUserId(1);
    book.setUsername("Maruko");
    book.setUserStatus("onlines");
    bookService.updateBook(book);
  }


  public void testDelete() {
    ApplicationContext context = new ClassPathXmlApplicationContext("bean1.xml");
    BookService bookService = context.getBean("bookService", BookService.class);
    bookService.deleteBook("1");
  }


  public void testCount() {
    ApplicationContext context = new ClassPathXmlApplicationContext("bean1.xml");
    BookService bookService = context.getBean("bookService", BookService.class);
    bookService.selectCount();
  }


  public void testBatchAddBooks() {
    ApplicationContext context = new ClassPathXmlApplicationContext("bean1.xml");
    BookService bookService = context.getBean("bookService", BookService.class);
    List<Object[]> books = new ArrayList<>();
    Object[] o1 = {"3", "java", "a"};
    Object[] o2 = {"5", "C", "b"};
    books.add(o1);
    books.add(o2);
    bookService.batchAddBook(books);
  }


  public void testBatchUpdate() {
    ApplicationContext context = new ClassPathXmlApplicationContext("bean1.xml");
    BookService bookService = context.getBean("bookService", BookService.class);
    List<Object[]> books = new ArrayList<>();
    Object[] o1 = {"3", "Micah", "3"};
    Object[] o2 = {"5", "Maruko", "5"};
    books.add(o1);
    books.add(o2);
    bookService.batchUpdateBook(books);
  }


  public void testBatchDelete() {
    ApplicationContext context = new ClassPathXmlApplicationContext("bean1.xml");
    BookService bookService = context.getBean("bookService", BookService.class);
    List<Object[]> ids = new ArrayList<>();
    Object[] o1 = {"3"};
    Object[] o2 = {"5"};
    ids.add(o1);
    ids.add(o2);
    bookService.batchDelBook(ids);
  }
}
