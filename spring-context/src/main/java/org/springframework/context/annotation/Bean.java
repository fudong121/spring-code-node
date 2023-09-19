/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.core.annotation.AliasFor;

/**
 * Indicates that a method produces a bean to be managed by the Spring container.
 *
 * <h3>Overview</h3>
 *
 * <p>The names and semantics of the attributes to this annotation are intentionally similar to
 * those of the {@code <bean/>} element in the Spring XML schema. For example:
 *
 * <pre class="code">
 *     &#064;Bean
 *     public MyBean myBean() {
 *         // instantiate and configure MyBean obj
 *         return obj;
 *     }
 * </pre>
 *
 * <h3>Bean Names</h3>
 *
 * <p>While a {@link #name} attribute is available, the default strategy for determining the name of
 * a bean is to use the name of the {@code @Bean} method. This is convenient and intuitive, but if
 * explicit naming is desired, the {@code name} attribute (or its alias {@code value}) may be used.
 * Also note that {@code name} accepts an array of Strings, allowing for multiple names (i.e. a
 * primary bean name plus one or more aliases) for a single bean.
 *
 * <pre class="code">
 *     &#064;Bean({"b1", "b2"}) // bean available as 'b1' and 'b2', but not 'myBean'
 *     public MyBean myBean() {
 *         // instantiate and configure MyBean obj
 *         return obj;
 *     }
 * </pre>
 *
 * <h3>Profile, Scope, Lazy, DependsOn, Primary, Order</h3>
 *
 * <p>Note that the {@code @Bean} annotation does not provide attributes for profile, scope, lazy,
 * depends-on or primary. Rather, it should be used in conjunction with {@link Scope @Scope}, {@link
 * Lazy @Lazy}, {@link DependsOn @DependsOn} and {@link Primary @Primary} annotations to declare
 * those semantics. For example:
 *
 * <pre class="code">
 *     &#064;Bean
 *     &#064;Profile("production")
 *     &#064;Scope("prototype")
 *     public MyBean myBean() {
 *         // instantiate and configure MyBean obj
 *         return obj;
 *     }
 * </pre>
 *
 * The semantics of the above-mentioned annotations match their use at the component class level:
 * {@code @Profile} allows for selective inclusion of certain beans. {@code @Scope} changes the
 * bean's scope from singleton to the specified scope. {@code @Lazy} only has an actual effect in
 * case of the default singleton scope. {@code @DependsOn} enforces the creation of specific other
 * beans before this bean will be created, in addition to any dependencies that the bean expressed
 * through direct references, which is typically helpful for singleton startup. {@code @Primary} is
 * a mechanism to resolve ambiguity at the injection point level if a single target component needs
 * to be injected but several beans match by type.
 *
 * <p>Additionally, {@code @Bean} methods may also declare qualifier annotations and {@link
 * org.springframework.core.annotation.Order @Order} values, to be taken into account during
 * injection point resolution just like corresponding annotations on the corresponding component
 * classes but potentially being very individual per bean definition (in case of multiple
 * definitions with the same bean class). Qualifiers narrow the set of candidates after the initial
 * type match; order values determine the order of resolved elements in case of collection injection
 * points (with several target beans matching by type and qualifier).
 *
 * <p><b>NOTE:</b> {@code @Order} values may influence priorities at injection points, but please be
 * aware that they do not influence singleton startup order which is an orthogonal concern
 * determined by dependency relationships and {@code @DependsOn} declarations as mentioned above.
 * Also, {@link javax.annotation.Priority} is not available at this level since it cannot be
 * declared on methods; its semantics can be modeled through {@code @Order} values in combination
 * with {@code @Primary} on a single bean per type.
 *
 * <h3>{@code @Bean} Methods in {@code @Configuration} Classes</h3>
 *
 * <p>Typically, {@code @Bean} methods are declared within {@code @Configuration} classes. In this
 * case, bean methods may reference other {@code @Bean} methods in the same class by calling them
 * <i>directly</i>. This ensures that references between beans are strongly typed and navigable.
 * Such so-called <em>'inter-bean references'</em> are guaranteed to respect scoping and AOP
 * semantics, just like {@code getBean()} lookups would. These are the semantics known from the
 * original 'Spring JavaConfig' project which require CGLIB subclassing of each such configuration
 * class at runtime. As a consequence, {@code @Configuration} classes and their factory methods must
 * not be marked as final or private in this mode. For example:
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public FooService fooService() {
 *         return new FooService(fooRepository());
 *     }
 *
 *     &#064;Bean
 *     public FooRepository fooRepository() {
 *         return new JdbcFooRepository(dataSource());
 *     }
 *
 *     // ...
 * }</pre>
 *
 * <h3>{@code @Bean} <em>Lite</em> Mode</h3>
 *
 * <p>{@code @Bean} methods may also be declared within classes that are <em>not</em> annotated with
 * {@code @Configuration}. For example, bean methods may be declared in a {@code @Component} class
 * or even in a <em>plain old class</em>. In such cases, a {@code @Bean} method will get processed
 * in a so-called <em>'lite'</em> mode.
 *
 * <p>Bean methods in <em>lite</em> mode will be treated as plain <em>factory methods</em> by the
 * container (similar to {@code factory-method} declarations in XML), with scoping and lifecycle
 * callbacks properly applied. The containing class remains unmodified in this case, and there are
 * no unusual constraints for the containing class or the factory methods.
 *
 * <p>In contrast to the semantics for bean methods in {@code @Configuration} classes,
 * <em>'inter-bean references'</em> are not supported in <em>lite</em> mode. Instead, when one
 * {@code @Bean}-method invokes another {@code @Bean}-method in <em>lite</em> mode, the invocation
 * is a standard Java method invocation; Spring does not intercept the invocation via a CGLIB proxy.
 * This is analogous to inter-{@code @Transactional} method calls where in proxy mode, Spring does
 * not intercept the invocation &mdash; Spring does so only in AspectJ mode.
 *
 * <p>For example:
 *
 * <pre class="code">
 * &#064;Component
 * public class Calculator {
 *     public int sum(int a, int b) {
 *         return a+b;
 *     }
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         return new MyBean();
 *     }
 * }</pre>
 *
 * <h3>Bootstrapping</h3>
 *
 * <p>See the @{@link Configuration} javadoc for further details including how to bootstrap the
 * container using {@link AnnotationConfigApplicationContext} and friends.
 *
 * <h3>{@code BeanFactoryPostProcessor}-returning {@code @Bean} methods</h3>
 *
 * <p>Special consideration must be taken for {@code @Bean} methods that return Spring {@link
 * org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessor}
 * ({@code BFPP}) types. Because {@code BFPP} objects must be instantiated very early in the
 * container lifecycle, they can interfere with processing of annotations such as
 * {@code @Autowired}, {@code @Value}, and {@code @PostConstruct} within {@code @Configuration}
 * classes. To avoid these lifecycle issues, mark {@code BFPP}-returning {@code @Bean} methods as
 * {@code static}. For example:
 *
 * <pre class="code">
 *     &#064;Bean
 *     public static PropertySourcesPlaceholderConfigurer pspc() {
 *         // instantiate, configure and return pspc ...
 *     }
 * </pre>
 *
 * By marking this method as {@code static}, it can be invoked without causing instantiation of its
 * declaring {@code @Configuration} class, thus avoiding the above-mentioned lifecycle conflicts.
 * Note however that {@code static} {@code @Bean} methods will not be enhanced for scoping and AOP
 * semantics as mentioned above. This works out in {@code BFPP} cases, as they are not typically
 * referenced by other {@code @Bean} methods. As a reminder, a WARN-level log message will be issued
 * for any non-static {@code @Bean} methods having a return type assignable to {@code
 * BeanFactoryPostProcessor}.
 *
 * @author Rod Johnson
 * @author Costin Leau
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see Configuration
 * @see Scope
 * @see DependsOn
 * @see Lazy
 * @see Primary
 * @see org.springframework.stereotype.Component
 * @see org.springframework.beans.factory.annotation.Autowired
 * @see org.springframework.beans.factory.annotation.Value
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Bean {

  /**
   * Alias for {@link #name}.
   *
   * <p>Intended to be used when no other attributes are needed, for example:
   * {@code @Bean("customBeanName")}.
   *
   * @since 4.3.3
   * @see #name
   */
  @AliasFor("name")
  String[] value() default {};

  /**
   * The name of this bean, or if several names, a primary bean name plus aliases.
   *
   * <p>If left unspecified, the name of the bean is the name of the annotated method. If specified,
   * the method name is ignored.
   *
   * <p>The bean name and aliases may also be configured via the {@link #value} attribute if no
   * other attributes are declared.
   *
   * @see #value
   */
  @AliasFor("value")
  String[] name() default {};

  /**
   * Are dependencies to be injected via convention-based autowiring by name or type?
   *
   * <p>Note that this autowire mode is just about externally driven autowiring based on bean
   * property setter methods by convention, analogous to XML bean definitions.
   *
   * <p>The default mode does allow for annotation-driven autowiring. "no" refers to externally
   * driven autowiring only, not affecting any autowiring demands that the bean class itself
   * expresses through annotations.
   *
   * @see Autowire#BY_NAME
   * @see Autowire#BY_TYPE
   * @deprecated as of 5.1, since {@code @Bean} factory method argument resolution and
   *     {@code @Autowired} processing supersede name/type-based bean property injection
   */
  @Deprecated
  Autowire autowire() default Autowire.NO;

  /**
   * Is this bean a candidate for getting autowired into some other bean?
   *
   * <p>Default is {@code true}; set this to {@code false} for internal delegates that are not meant
   * to get in the way of beans of the same type in other places.
   *
   * @since 5.1
   */
  boolean autowireCandidate() default true;

  /**
   * The optional name of a method to call on the bean instance during initialization. Not commonly
   * used, given that the method may be called programmatically directly within the body of a
   * Bean-annotated method.
   *
   * <p>The default value is {@code ""}, indicating no init method to be called.
   *
   * @see org.springframework.beans.factory.InitializingBean
   * @see org.springframework.context.ConfigurableApplicationContext#refresh()
   */
  String initMethod() default "";

  /**
   * 在关闭应用程序上下文时在bean实例上调用的方法的可选名称，例如JDBC {@code DataSource}实现上的{@code close()}方法，或Hibernate {@code
   * SessionFactory}对象。该方法必须没有参数，但可以抛出任何异常。
   *
   * <p>为了方便用户，容器将尝试根据{@code @Bean}方法返回的对象推断出destroy方法。例如，给定一个{@code @Bean}方法返回Apache Commons DBCP
   * {@code BasicDataSource}，容器将注意到该对象上可用的{@code close()}方法，并自动将其注册为{@code
   * destroyMethod}。这种“销毁方法推理”目前仅限于检测名为“close”或“shutdown”的公共无参数方法。该方法可以在继承层次的任何级别上声明，并且无论{@code @Bean}方法的返回类型如何都将被检测到(即，在创建时对bean实例本身进行反射性检测)。
   *
   * <p>要为特定的{@code @Bean}禁用destroy方法推理，请指定一个空字符串作为值，例如{@code @Bean(destroyMethod="")}。注意，{@link
   * org.springframework.beans.factory.然而，DisposableBean}回调接口将被检测到，并调用相应的destroy方法:换句话说，{@code
   * destroyMethod=""}只影响自定义的closeshutdown方法和{@link java.io。Closeable} {@link .
   * lang。AutoCloseable}声明了关闭方法。
   *
   * <p>注意:仅在生命周期处于工厂完全控制下的bean上调用，这对于单例例总是如此，但对于任何其他作用域都不能保证
   *
   * @see org.springframework.beans.factory.DisposableBean
   * @see org.springframework.context.ConfigurableApplicationContext#close()
   */
  String destroyMethod() default AbstractBeanDefinition.INFER_METHOD;
}
