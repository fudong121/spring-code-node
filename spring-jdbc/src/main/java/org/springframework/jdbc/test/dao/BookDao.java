package org.springframework.jdbc.test.dao;

import java.util.List;
import org.springframework.jdbc.test.enity.Book;

/**
 * @Author m.kong
 * @Date 2021/2/28 下午9:20
 * @Version 1.0
 */
public interface BookDao {
    /**
     * 添加add
     */
    void add(Book book);
    /**
     * update
     */
    void update(Book book);

    /**
     * delete
     */
    void deleteBook(String id);

    /**
     * count(*)
     */
    int selectCount();

    /**
     * batchAddBooks
     * @param books
     */
    void batchAddBook(List<Object[]> books);

    void batchUpdateBook(List<Object[]> books);

    void batchDelBook(List<Object[]> ids);
}
