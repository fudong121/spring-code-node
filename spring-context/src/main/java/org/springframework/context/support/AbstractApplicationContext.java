/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.context.support;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.CachedIntrospectionResults;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.support.ResourceEditorRegistrar;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.LifecycleProcessor;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.context.weaving.LoadTimeWeaverAware;
import org.springframework.context.weaving.LoadTimeWeaverAwareProcessor;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * 抽象实现了{@link
 * org.springframework.context。ApplicationContext}接口。不强制配置使用的存储类型;简单地实现公共上下文功能。使用模板方法设计模式，需要具体的子类来实现抽象方法。
 *
 * <p>与普通的BeanFactory相比，ApplicationContext应该检测在其内部bean工厂中定义的特殊bean:因此，这个类自动注册{@link
 * org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessors}，
 * {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessors}和{@link
 * org.springframework.context.ApplicationListener ApplicationListeners}，它们在上下文中被定义为bean。
 *
 * <p>A {@link org.springframework.context.MessageSource} may also be supplied as a bean in the
 * context, with the name "messageSource"; otherwise, message resolution is delegated to the parent
 * context. Furthermore, a multicaster for application events can be supplied as an
 * "applicationEventMulticaster" bean of type {@link
 * org.springframework.context.event.ApplicationEventMulticaster} in the context; otherwise, a
 * default multicaster of type {@link
 * org.springframework.context.event.SimpleApplicationEventMulticaster} will be used.
 *
 * <p>Implements resource loading by extending {@link
 * org.springframework.core.io.DefaultResourceLoader}. Consequently treats non-URL resource paths as
 * class path resources (supporting full class path resource names that include the package path,
 * e.g. "mypackage/myresource.dat"), unless the {@link #getResourceByPath} method is overridden in a
 * subclass.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since January 21, 2001
 * @see #refreshBeanFactory
 * @see #getBeanFactory
 * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor
 * @see org.springframework.beans.factory.config.BeanPostProcessor
 * @see org.springframework.context.event.ApplicationEventMulticaster
 * @see org.springframework.context.ApplicationListener
 * @see org.springframework.context.MessageSource
 */
public abstract class AbstractApplicationContext extends DefaultResourceLoader
    implements ConfigurableApplicationContext {

  /**
   * Name of the MessageSource bean in the factory. If none is supplied, message resolution is
   * delegated to the parent.
   *
   * @see MessageSource
   */
  public static final String MESSAGE_SOURCE_BEAN_NAME = "messageSource";

  /**
   * Name of the LifecycleProcessor bean in the factory. If none is supplied, a
   * DefaultLifecycleProcessor is used.
   *
   * @see org.springframework.context.LifecycleProcessor
   * @see org.springframework.context.support.DefaultLifecycleProcessor
   */
  public static final String LIFECYCLE_PROCESSOR_BEAN_NAME = "lifecycleProcessor";

  /**
   * Name of the ApplicationEventMulticaster bean in the factory. If none is supplied, a default
   * SimpleApplicationEventMulticaster is used.
   *
   * @see org.springframework.context.event.ApplicationEventMulticaster
   * @see org.springframework.context.event.SimpleApplicationEventMulticaster
   */
  public static final String APPLICATION_EVENT_MULTICASTER_BEAN_NAME =
      "applicationEventMulticaster";

  static {
    // Eagerly load the ContextClosedEvent class to avoid weird classloader issues
    // on application shutdown in WebLogic 8.1. (Reported by Dustin Woods.)
    ContextClosedEvent.class.getName();
  }

  /** Logger used by this class. Available to subclasses. */
  protected final Log logger = LogFactory.getLog(getClass());

  /** Unique id for this context, if any. */
  private String id = ObjectUtils.identityToString(this);

  /** Display name. */
  private String displayName = ObjectUtils.identityToString(this);

  /** Parent context. */
  @Nullable private ApplicationContext parent;

  /** Environment used by this context. */
  @Nullable private ConfigurableEnvironment environment;

  /** BeanFactoryPostProcessors to apply on refresh. */
  private final List<BeanFactoryPostProcessor> beanFactoryPostProcessors = new ArrayList<>();

  /** System time in milliseconds when this context started. */
  private long startupDate;

  /** Flag that indicates whether this context is currently active. */
  private final AtomicBoolean active = new AtomicBoolean();

  /** Flag that indicates whether this context has been closed already. */
  private final AtomicBoolean closed = new AtomicBoolean();

  /** “刷新”和“销毁”的同步监视器. */
  private final Object startupShutdownMonitor = new Object();

  /** Reference to the JVM shutdown hook, if registered. */
  @Nullable private Thread shutdownHook;

  /** ResourcePatternResolver used by this context. */
  private ResourcePatternResolver resourcePatternResolver;

  /** LifecycleProcessor for managing the lifecycle of beans within this context. */
  @Nullable private LifecycleProcessor lifecycleProcessor;

  /** MessageSource we delegate our implementation of this interface to. */
  @Nullable private MessageSource messageSource;

  /** Helper class used in event publishing. */
  @Nullable private ApplicationEventMulticaster applicationEventMulticaster;

  /** Statically specified listeners. */
  private final Set<ApplicationListener<?>> applicationListeners = new LinkedHashSet<>();

  /** Local listeners registered before refresh. */
  @Nullable private Set<ApplicationListener<?>> earlyApplicationListeners;

  /** ApplicationEvents published before the multicaster setup. */
  @Nullable private Set<ApplicationEvent> earlyApplicationEvents;

  /** Create a new AbstractApplicationContext with no parent. */
  public AbstractApplicationContext() {
    this.resourcePatternResolver = getResourcePatternResolver();
  }

  /**
   * Create a new AbstractApplicationContext with the given parent context.
   *
   * @param parent the parent context
   */
  public AbstractApplicationContext(@Nullable ApplicationContext parent) {
    this();
    setParent(parent);
  }

  // ---------------------------------------------------------------------
  // Implementation of ApplicationContext interface
  // ---------------------------------------------------------------------

  /**
   * Set the unique id of this application context.
   *
   * <p>Default is the object id of the context instance, or the name of the context bean if the
   * context is itself defined as a bean.
   *
   * @param id the unique id of the context
   */
  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String getId() {
    return this.id;
  }

  @Override
  public String getApplicationName() {
    return "";
  }

  /**
   * Set a friendly name for this context. Typically done during initialization of concrete context
   * implementations.
   *
   * <p>Default is the object id of the context instance.
   */
  public void setDisplayName(String displayName) {
    Assert.hasLength(displayName, "Display name must not be empty");
    this.displayName = displayName;
  }

  /**
   * Return a friendly name for this context.
   *
   * @return a display name for this context (never {@code null})
   */
  @Override
  public String getDisplayName() {
    return this.displayName;
  }

  /**
   * Return the parent context, or {@code null} if there is no parent (that is, this context is the
   * root of the context hierarchy).
   */
  @Override
  @Nullable
  public ApplicationContext getParent() {
    return this.parent;
  }

  /**
   * Set the {@code Environment} for this application context.
   *
   * <p>Default value is determined by {@link #createEnvironment()}. Replacing the default with this
   * method is one option but configuration through {@link #getEnvironment()} should also be
   * considered. In either case, such modifications should be performed <em>before</em> {@link
   * #refresh()}.
   *
   * @see org.springframework.context.support.AbstractApplicationContext#createEnvironment
   */
  @Override
  public void setEnvironment(ConfigurableEnvironment environment) {
    this.environment = environment;
  }

  /**
   * Return the {@code Environment} for this application context in configurable form, allowing for
   * further customization.
   *
   * <p>If none specified, a default environment will be initialized via {@link
   * #createEnvironment()}.
   */
  @Override
  public ConfigurableEnvironment getEnvironment() {
    if (this.environment == null) {
      this.environment = createEnvironment();
    }
    return this.environment;
  }

  /**
   * Create and return a new {@link StandardEnvironment}.
   *
   * <p>Subclasses may override this method in order to supply a custom {@link
   * ConfigurableEnvironment} implementation.
   */
  protected ConfigurableEnvironment createEnvironment() {
    return new StandardEnvironment();
  }

  /**
   * Return this context's internal bean factory as AutowireCapableBeanFactory, if already
   * available.
   *
   * @see #getBeanFactory()
   */
  @Override
  public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
    return getBeanFactory();
  }

  /** Return the timestamp (ms) when this context was first loaded. */
  @Override
  public long getStartupDate() {
    return this.startupDate;
  }

  /**
   * Publish the given event to all listeners.
   *
   * <p>Note: Listeners get initialized after the MessageSource, to be able to access it within
   * listener implementations. Thus, MessageSource implementations cannot publish events.
   *
   * @param event the event to publish (may be application-specific or a standard framework event)
   */
  @Override
  public void publishEvent(ApplicationEvent event) {
    publishEvent(event, null);
  }

  /**
   * Publish the given event to all listeners.
   *
   * <p>Note: Listeners get initialized after the MessageSource, to be able to access it within
   * listener implementations. Thus, MessageSource implementations cannot publish events.
   *
   * @param event the event to publish (may be an {@link ApplicationEvent} or a payload object to be
   *     turned into a {@link PayloadApplicationEvent})
   */
  @Override
  public void publishEvent(Object event) {
    publishEvent(event, null);
  }

  /**
   * Publish the given event to all listeners.
   *
   * @param event the event to publish (may be an {@link ApplicationEvent} or a payload object to be
   *     turned into a {@link PayloadApplicationEvent})
   * @param eventType the resolved event type, if known
   * @since 4.2
   */
  protected void publishEvent(Object event, @Nullable ResolvableType eventType) {
    Assert.notNull(event, "Event must not be null");

    // Decorate event as an ApplicationEvent if necessary
    ApplicationEvent applicationEvent;
    if (event instanceof ApplicationEvent) {
      applicationEvent = (ApplicationEvent) event;
    } else {
      applicationEvent = new PayloadApplicationEvent<>(this, event);
      if (eventType == null) {
        eventType = ((PayloadApplicationEvent<?>) applicationEvent).getResolvableType();
      }
    }

    // Multicast right now if possible - or lazily once the multicaster is initialized
    if (this.earlyApplicationEvents != null) {
      this.earlyApplicationEvents.add(applicationEvent);
    } else {
      getApplicationEventMulticaster().multicastEvent(applicationEvent, eventType);
    }

    // Publish event via parent context as well...
    if (this.parent != null) {
      if (this.parent instanceof AbstractApplicationContext) {
        ((AbstractApplicationContext) this.parent).publishEvent(event, eventType);
      } else {
        this.parent.publishEvent(event);
      }
    }
  }

  /**
   * Return the internal ApplicationEventMulticaster used by the context.
   *
   * @return the internal ApplicationEventMulticaster (never {@code null})
   * @throws IllegalStateException if the context has not been initialized yet
   */
  ApplicationEventMulticaster getApplicationEventMulticaster() throws IllegalStateException {
    if (this.applicationEventMulticaster == null) {
      throw new IllegalStateException(
          "ApplicationEventMulticaster not initialized - "
              + "call 'refresh' before multicasting events via the context: "
              + this);
    }
    return this.applicationEventMulticaster;
  }

  /**
   * Return the internal LifecycleProcessor used by the context.
   *
   * @return the internal LifecycleProcessor (never {@code null})
   * @throws IllegalStateException if the context has not been initialized yet
   */
  LifecycleProcessor getLifecycleProcessor() throws IllegalStateException {
    if (this.lifecycleProcessor == null) {
      throw new IllegalStateException(
          "LifecycleProcessor not initialized - "
              + "call 'refresh' before invoking lifecycle methods via the context: "
              + this);
    }
    return this.lifecycleProcessor;
  }

  /**
   * Return the ResourcePatternResolver to use for resolving location patterns into Resource
   * instances. Default is a {@link
   * org.springframework.core.io.support.PathMatchingResourcePatternResolver}, supporting Ant-style
   * location patterns.
   *
   * <p>Can be overridden in subclasses, for extended resolution strategies, for example in a web
   * environment.
   *
   * <p><b>Do not call this when needing to resolve a location pattern.</b> Call the context's
   * {@code getResources} method instead, which will delegate to the ResourcePatternResolver.
   *
   * @return the ResourcePatternResolver for this context
   * @see #getResources
   * @see org.springframework.core.io.support.PathMatchingResourcePatternResolver
   */
  protected ResourcePatternResolver getResourcePatternResolver() {
    return new PathMatchingResourcePatternResolver(this);
  }

  // ---------------------------------------------------------------------
  // Implementation of ConfigurableApplicationContext interface
  // ---------------------------------------------------------------------

  /**
   * Set the parent of this application context.
   *
   * <p>The parent {@linkplain ApplicationContext#getEnvironment() environment} is {@linkplain
   * ConfigurableEnvironment#merge(ConfigurableEnvironment) merged} with this (child) application
   * context environment if the parent is non-{@code null} and its environment is an instance of
   * {@link ConfigurableEnvironment}.
   *
   * @see ConfigurableEnvironment#merge(ConfigurableEnvironment)
   */
  @Override
  public void setParent(@Nullable ApplicationContext parent) {
    this.parent = parent;
    if (parent != null) {
      Environment parentEnvironment = parent.getEnvironment();
      if (parentEnvironment instanceof ConfigurableEnvironment) {
        getEnvironment().merge((ConfigurableEnvironment) parentEnvironment);
      }
    }
  }

  @Override
  public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor) {
    Assert.notNull(postProcessor, "BeanFactoryPostProcessor must not be null");
    this.beanFactoryPostProcessors.add(postProcessor);
  }

  /**
   * Return the list of BeanFactoryPostProcessors that will get applied to the internal BeanFactory.
   */
  public List<BeanFactoryPostProcessor> getBeanFactoryPostProcessors() {
    return this.beanFactoryPostProcessors;
  }

  @Override
  public void addApplicationListener(ApplicationListener<?> listener) {
    Assert.notNull(listener, "ApplicationListener must not be null");
    if (this.applicationEventMulticaster != null) {
      this.applicationEventMulticaster.addApplicationListener(listener);
    }
    this.applicationListeners.add(listener);
  }

  /** 返回静态指定的ApplicationListeners列表. */
  public Collection<ApplicationListener<?>> getApplicationListeners() {
    return this.applicationListeners;
  }

  @Override
  public void refresh() throws BeansException, IllegalStateException {
    synchronized (this.startupShutdownMonitor) {
      // 1 刷新前的预处理。准备此上下文以进行刷新(前戏)
      prepareRefresh();

      // 2 获取BeanFactory；刚创建的默认DefaultListableBeanFactory
      ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

      // 3 BeanFactory的预准备工作（BeanFactory进行一些设置）。
      prepareBeanFactory(beanFactory);

      try {
        // 4 BeanFactory准备工作完成后进行的后置处理工作；
        postProcessBeanFactory(beanFactory);
        /**************************以上是BeanFactory的创建及预准备工作  ****************/
        // 5 执行BeanFactoryPostProcessor的方法；
        invokeBeanFactoryPostProcessors(beanFactory);

        // 6 注册BeanPostProcessor（Bean的后置处理器）
        registerBeanPostProcessors(beanFactory);

        // 7 initMessageSource();初始化MessageSource组件（做国际化功能；消息绑定，消息解析）；
        initMessageSource();

        // 8 初始化事件派发器
        initApplicationEventMulticaster();

        // 9 子类重写这个方法，在容器刷新的时候可以自定义逻辑；
        onRefresh();

        // 10 给容器中将所有项目里面的ApplicationListener注册进来
        registerListeners();

        // 11.初始化所有剩下的单实例bean；
        finishBeanFactoryInitialization(beanFactory);

        // 最后一步:发布相应的事件。
        finishRefresh();
      } catch (BeansException ex) {
        if (logger.isWarnEnabled()) {
          logger.warn("在上下文初始化过程中遇到异常 - " + "取消刷新尝试: " + ex);
        }

        // 销毁已经创建的单例以避免悬空资源
        destroyBeans();

        // 重置“活动”标志。
        cancelRefresh(ex);

        // 将异常传播给调用者。
        throw ex;
      } finally {
        // 在Spring的核心中重置常见的自省缓存，因为我们
        // 可能再也不需要单例bean的元数据了……
        resetCommonCaches();
      }
    }
    /*
        ====================================总结=================
         1）、Spring容器在启动的时候，先会保存所有注册进来的Bean的定义信息；
         	1）、xml注册bean；<bean>
           	2）、注解注册Bean；@Service、@Component、@Bean、xxx
         2）、Spring容器会合适的时机创建这些Bean
         	 1）、用到这个bean的时候；利用getBean创建bean；创建好以后保存在容器中；
         	 2）、统一创建剩下所有的bean的时候；finishBeanFactoryInitialization()；
         3）、后置处理器；BeanPostProcessor
         	1）、每一个bean创建完成，都会使用各种后置处理器进行处理；来增强bean的功能；
         AutowiredAnnotationBeanPostProcessor:处理自动注入
         AnnotationAwareAspectJAutoProxyCreator:来做AOP功能；
         xxx....
         增强的功能注解：
          AsyncAnnotationBeanPostProcessor
                             ....
         4）、事件驱动模型；
            ApplicationListener；事件监听；
            ApplicationEventMulticaster；事件派发：

           ===================Bean生命周期主要为四个阶段：==================
                      实例化
                      属性赋值
                      初始化
                      销毁
           ==========================自动装配============================
                  /**
                   * 自动装配;
                   * 		Spring利用依赖注入（DI），完成对IOC容器中中各个组件的依赖关系赋值；
                   *
                   * 1）、@Autowired：自动注入：
                   * 		1）、默认优先按照类型去容器中找对应的组件:applicationContext.getBean(BookDao.class);找到就赋值
                   * 		2）、如果找到多个相同类型的组件，再将属性的名称作为组件的id去容器中查找
                   * 							applicationContext.getBean("bookDao")
                   * 		3）、@Qualifier("bookDao")：使用@Qualifier指定需要装配的组件的id，而不是使用属性名
                   * 		4）、自动装配默认一定要将属性赋值好，没有就会报错；
                   * 			可以使用@Autowired(required=false);
                   * 		5）、@Primary：让Spring进行自动装配的时候，默认使用首选的bean；
                   * 				也可以继续使用@Qualifier指定需要装配的bean的名字
                   * 		BookService{
                   * 			@Autowired
                   * 			BookDao  bookDao;
                   * 		}
                   *
                   * 2）、Spring还支持使用@Resource(JSR250)和@Inject(JSR330)[java规范的注解]
                   * 		@Resource:
                   * 			可以和@Autowired一样实现自动装配功能；默认是按照组件名称进行装配的；
                   * 			没有能支持@Primary功能没有支持@Autowired（reqiured=false）;
                   * 		@Inject:
                   * 			需要导入javax.inject的包，和Autowired的功能一样。没有required=false的功能；
                   *  @Autowired:Spring定义的； @Resource、@Inject都是java规范
                   *
                   * AutowiredAnnotationBeanPostProcessor:解析完成自动装配功能；
                   *
                   * 3）、 @Autowired:构造器，参数，方法，属性；都是从容器中获取参数组件的值
                   * 		1）、[标注在方法位置]：@Bean+方法参数；参数从容器中获取;默认不写@Autowired效果是一样的；都能自动装配
                   * 		2）、[标在构造器上]：如果组件只有一个有参构造器，这个有参构造器的@Autowired可以省略，参数位置的组件还是可以自动从容器中获取
                   * 		3）、放在参数位置：
                   *
                   * 4）、自定义组件想要使用Spring容器底层的一些组件（ApplicationContext，BeanFactory，xxx）；
                   * 		自定义组件实现xxxAware；在创建对象的时候，会调用接口规定的方法注入相关组件；Aware；
                   * 		把Spring底层一些组件注入到自定义的Bean中；
                   * 		xxxAware：功能使用xxxProcessor；
                   * 			ApplicationContextAware==》ApplicationContextAwareProcessor；
                   *
                   *
                   *
                   *
           ===================================Bean生命周期==============================
             1、 读取BeanDefinition信息
             2、 注册各种BeanPostProcessor
             3、 实例化之前先执行InstantiationAwareBeanPostProcessor，执行applyBeanPostProcessorsBeforeInstantiation方法，可以返回Bean的代理对象
             4、 利用反射实例化Bean
             5、 实例化完成后，调用MergedBeanDefinitionPostProcessor的postProcessMergedBeanDefinition(mbd, beanType, beanName);
             6、 然后调用InstantiationAwareBeanPostProcessor的postProcessAfterInstantiation方法
             7、 执行populateBean方法，对Bean进行属性赋值
             8、 如果Bean实现了XXXAware接口，就为Bean设置上这些属性，首先执行的是BeanNameAware、BeanClassLoaderAware、BeanFactoryAware
             9、 执行BeanPostProcessor.postProcessBeforeInitialization方法
             10、 如果标注了@PostConstruct就先执行这个初始化方法；如果实现了InitializingBean，就再执行这个初始化方法；然后执行自定义的初始化方法。
             11、 初始化完成后，执行BeanPostProcessor.postProcessAfterInitialization()
             12、 如果容器关闭，就依次执行@PreDestory、DisposableBean、destroyMethod等销毁方法


    =============================AOP===============================
    /**
     * AOP：【动态代理】
     * 		指在程序运行期间动态的将某段代码切入到指定方法指定位置进行运行的编程方式；
     *
     * 1、导入aop模块；Spring AOP：(spring-aspects)
     * 2、定义一个业务逻辑类（MathCalculator）；在业务逻辑运行的时候将日志进行打印（方法之前、方法运行结束、方法出现异常，xxx）
     * 3、定义一个日志切面类（LogAspects）：切面类里面的方法需要动态感知MathCalculator.div运行到哪里然后执行；
     * 		通知方法：
     * 			前置通知(@Before)：logStart：在目标方法(div)运行之前运行
     * 			后置通知(@After)：logEnd：在目标方法(div)运行结束之后运行（无论方法正常结束还是异常结束）
     * 			返回通知(@AfterReturning)：logReturn：在目标方法(div)正常返回之后运行
     * 			异常通知(@AfterThrowing)：logException：在目标方法(div)出现异常以后运行
     * 			环绕通知(@Around)：动态代理，手动推进目标方法运行（joinPoint.procced()）
     * 4、给切面类的目标方法标注何时何地运行（通知注解）；
     * 5、将切面类和业务逻辑类（目标方法所在类）都加入到容器中;
     * 6、必须告诉Spring哪个类是切面类(给切面类上加一个注解：@Aspect)
     * [7]、给配置类中加 @EnableAspectJAutoProxy 【开启基于注解的aop模式】
     * 		在Spring中很多的 @EnableXXX;
     *
     * 三步：
     * 	1）、将业务逻辑组件和切面类都加入到容器中；告诉Spring哪个是切面类（@Aspect）
     * 	2）、在切面类上的每一个通知方法上标注通知注解，告诉Spring何时何地运行（切入点表达式）
     *  3）、开启基于注解的aop模式；@EnableAspectJAutoProxy
     *
     * AOP原理：【看给容器中注册了什么组件，这个组件什么时候工作，这个组件的功能是什么？】
     * 		@EnableAspectJAutoProxy；
     * 1、@EnableAspectJAutoProxy是什么？
     * 		@Import(AspectJAutoProxyRegistrar.class)：给容器中导入AspectJAutoProxyRegistrar
     * 			利用AspectJAutoProxyRegistrar自定义给容器中注册bean；BeanDefinetion
     * 			internalAutoProxyCreator=AnnotationAwareAspectJAutoProxyCreator
     *
     * 		给容器中注册一个AnnotationAwareAspectJAutoProxyCreator；
     *
     * 2、 AnnotationAwareAspectJAutoProxyCreator：
     * 		AnnotationAwareAspectJAutoProxyCreator
     * 			->AspectJAwareAdvisorAutoProxyCreator
     * 				->AbstractAdvisorAutoProxyCreator
     * 					->AbstractAutoProxyCreator
     * 							implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware
     * 						关注后置处理器（在bean初始化完成前后做事情）、自动装配BeanFactory
     *
     * AbstractAutoProxyCreator.setBeanFactory()
     * AbstractAutoProxyCreator.有后置处理器的逻辑；
     *
     * AbstractAdvisorAutoProxyCreator.setBeanFactory()-》initBeanFactory()
     *
     * AnnotationAwareAspectJAutoProxyCreator.initBeanFactory()
     *
     *
     * 流程：
     * 		1）、传入配置类，创建ioc容器
     * 		2）、注册配置类，调用refresh（）刷新容器；
     * 		3）、registerBeanPostProcessors(beanFactory);注册bean的后置处理器来方便拦截bean的创建；
     * 			1）、先获取ioc容器已经定义了的需要创建对象的所有BeanPostProcessor
     * 			2）、给容器中加别的BeanPostProcessor
     * 			3）、优先注册实现了PriorityOrdered接口的BeanPostProcessor；
     * 			4）、再给容器中注册实现了Ordered接口的BeanPostProcessor；
     * 			5）、注册没实现优先级接口的BeanPostProcessor；
     * 			6）、注册BeanPostProcessor，实际上就是创建BeanPostProcessor对象，保存在容器中；
     * 				创建internalAutoProxyCreator的BeanPostProcessor【AnnotationAwareAspectJAutoProxyCreator】
     * 				1）、创建Bean的实例
     * 				2）、populateBean；给bean的各种属性赋值
     * 				3）、initializeBean：初始化bean；
     * 						1）、invokeAwareMethods()：处理Aware接口的方法回调
     * 						2）、applyBeanPostProcessorsBeforeInitialization()：应用后置处理器的postProcessBeforeInitialization（）
     * 						3）、invokeInitMethods()；执行自定义的初始化方法
     * 						4）、applyBeanPostProcessorsAfterInitialization()；执行后置处理器的postProcessAfterInitialization（）；
     * 				4）、BeanPostProcessor(AnnotationAwareAspectJAutoProxyCreator)创建成功；--》aspectJAdvisorsBuilder
     * 			7）、把BeanPostProcessor注册到BeanFactory中；
     * 				beanFactory.addBeanPostProcessor(postProcessor);
     * =======以上是创建和注册AnnotationAwareAspectJAutoProxyCreator的过程========
     *
     * 			AnnotationAwareAspectJAutoProxyCreator => InstantiationAwareBeanPostProcessor
     * 		4）、finishBeanFactoryInitialization(beanFactory);完成BeanFactory初始化工作；创建剩下的单实例bean
     * 			1）、遍历获取容器中所有的Bean，依次创建对象getBean(beanName);
     * 				getBean->doGetBean()->getSingleton()->
     * 			2）、创建bean
     * 				【AnnotationAwareAspectJAutoProxyCreator在所有bean创建之前会有一个拦截，InstantiationAwareBeanPostProcessor，会调用postProcessBeforeInstantiation()】
     * 				1）、先从缓存中获取当前bean，如果能获取到，说明bean是之前被创建过的，直接使用，否则再创建；
     * 					只要创建好的Bean都会被缓存起来
     * 				2）、createBean（）;创建bean；
     * 					AnnotationAwareAspectJAutoProxyCreator 会在任何bean创建之前先尝试返回bean的实例
     * 					【BeanPostProcessor是在Bean对象创建完成初始化前后调用的】
     * 					【InstantiationAwareBeanPostProcessor是在创建Bean实例之前先尝试用后置处理器返回对象的】
     * 					1）、resolveBeforeInstantiation(beanName, mbdToUse);解析BeforeInstantiation
     * 						希望后置处理器在此能返回一个代理对象；如果能返回代理对象就使用，如果不能就继续
     * 						1）、后置处理器先尝试返回对象；
     * 							bean = applyBeanPostProcessorsBeforeInstantiation（）：
     * 								拿到所有后置处理器，如果是InstantiationAwareBeanPostProcessor;
     * 								就执行postProcessBeforeInstantiation
     * 							if (bean != null) {
    bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
    }
     *
     * 					2）、doCreateBean(beanName, mbdToUse, args);真正的去创建一个bean实例；和3.6流程一样；
     * 					3）、
     *
     *
     * AnnotationAwareAspectJAutoProxyCreator【InstantiationAwareBeanPostProcessor】	的作用：
     * 1）、每一个bean创建之前，调用postProcessBeforeInstantiation()；
     * 		关心MathCalculator和LogAspect的创建
     * 		1）、判断当前bean是否在advisedBeans中（保存了所有需要增强bean）
     * 		2）、判断当前bean是否是基础类型的Advice、Pointcut、Advisor、AopInfrastructureBean，
     * 			或者是否是切面（@Aspect）
     * 		3）、是否需要跳过
     * 			1）、获取候选的增强器（切面里面的通知方法）【List<Advisor> candidateAdvisors】
     * 				每一个封装的通知方法的增强器是 InstantiationModelAwarePointcutAdvisor；
     * 				判断每一个增强器是否是 AspectJPointcutAdvisor 类型的；返回true
     * 			2）、永远返回false
     *
     * 2）、创建对象
     * postProcessAfterInitialization；
     * 		return wrapIfNecessary(bean, beanName, cacheKey);//包装如果需要的情况下
     * 		1）、获取当前bean的所有增强器（通知方法）  Object[]  specificInterceptors
     * 			1、找到候选的所有的增强器（找哪些通知方法是需要切入当前bean方法的）
     * 			2、获取到能在bean使用的增强器。
     * 			3、给增强器排序
     * 		2）、保存当前bean在advisedBeans中；
     * 		3）、如果当前bean需要增强，创建当前bean的代理对象；
     * 			1）、获取所有增强器（通知方法）
     * 			2）、保存到proxyFactory
     * 			3）、创建代理对象：Spring自动决定
     * 				JdkDynamicAopProxy(config);jdk动态代理；
     * 				ObjenesisCglibAopProxy(config);cglib的动态代理；
     * 		4）、给容器中返回当前组件使用cglib增强了的代理对象；
     * 		5）、以后容器中获取到的就是这个组件的代理对象，执行目标方法的时候，代理对象就会执行通知方法的流程；
     *
     *
     * 	3）、目标方法执行	；
     * 		容器中保存了组件的代理对象（cglib增强后的对象），这个对象里面保存了详细信息（比如增强器，目标对象，xxx）；
     * 		1）、CglibAopProxy.intercept();拦截目标方法的执行
     * 		2）、根据ProxyFactory对象获取将要执行的目标方法拦截器链；
     * 			List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);
     * 			1）、List<Object> interceptorList保存所有拦截器 5
     * 				一个默认的ExposeInvocationInterceptor 和 4个增强器；
     * 			2）、遍历所有的增强器，将其转为Interceptor；
     * 				registry.getInterceptors(advisor);
     * 			3）、将增强器转为List<MethodInterceptor>；
     * 				如果是MethodInterceptor，直接加入到集合中
     * 				如果不是，使用AdvisorAdapter将增强器转为MethodInterceptor；
     * 				转换完成返回MethodInterceptor数组；
     *
     * 		3）、如果没有拦截器链，直接执行目标方法;
     * 			拦截器链（每一个通知方法又被包装为方法拦截器，利用MethodInterceptor机制）
     * 		4）、如果有拦截器链，把需要执行的目标对象，目标方法，
     * 			拦截器链等信息传入创建一个 CglibMethodInvocation 对象，
     * 			并调用 Object retVal =  mi.proceed();
     * 		5）、拦截器链的触发过程;
     * 			1)、如果没有拦截器执行执行目标方法，或者拦截器的索引和拦截器数组-1大小一样（指定到了最后一个拦截器）执行目标方法；
     * 			2)、链式获取每一个拦截器，拦截器执行invoke方法，每一个拦截器等待下一个拦截器执行完成返回以后再来执行；
     * 				拦截器链的机制，保证通知方法与目标方法的执行顺序；
     *
     * 	总结：
     * 		1）、  @EnableAspectJAutoProxy 开启AOP功能
     * 		2）、 @EnableAspectJAutoProxy 会给容器中注册一个组件 AnnotationAwareAspectJAutoProxyCreator
     * 		3）、AnnotationAwareAspectJAutoProxyCreator是一个后置处理器；
     * 		4）、容器的创建流程：
     * 			1）、registerBeanPostProcessors（）注册后置处理器；创建AnnotationAwareAspectJAutoProxyCreator对象
     * 			2）、finishBeanFactoryInitialization（）初始化剩下的单实例bean
     * 				1）、创建业务逻辑组件和切面组件
     * 				2）、AnnotationAwareAspectJAutoProxyCreator拦截组件的创建过程
     * 				3）、组件创建完之后，判断组件是否需要增强
     * 					是：切面的通知方法，包装成增强器（Advisor）;给业务逻辑组件创建一个代理对象（cglib）；
     * 		5）、执行目标方法：
     * 			1）、代理对象执行目标方法
     * 			2）、CglibAopProxy.intercept()；
     * 				1）、得到目标方法的拦截器链（增强器包装成拦截器MethodInterceptor）
     * 				2）、利用拦截器的链式机制，依次进入每一个拦截器进行执行；
     * 				3）、效果：
     * 					正常执行：前置通知-》目标方法-》后置通知-》返回通知
     * 					出现异常：前置通知-》目标方法-》后置通知-》异常通知
     *
     *
     *
     */

  }

  /** 准备此上下文以进行刷新，设置其启动日期和活动标志，以及执行属性源的任何初始化。 */
  protected void prepareRefresh() {
    // 设置context容器的启动时间
    this.startupDate = System.currentTimeMillis();
    // 设置容器的当前状态
    this.closed.set(false);
    this.active.set(true);
    // 日志准备
    if (logger.isDebugEnabled()) {
      if (logger.isTraceEnabled()) {
        logger.trace("Refreshing " + this);
      } else {
        logger.debug("Refreshing " + getDisplayName());
      }
    }

    // 初始化属性，留给子类覆盖实现（子类自定义个性化的属性设置方法）
    initPropertySources();

    // 验证必要属性是否都已经被解析（校验属性的合法性）
    getEnvironment().validateRequiredProperties();

    if (this.earlyApplicationListeners == null) {
      // 新建一个LinkedHashSet目的为了保存容器中一些事件。（保存容器中的一些早期的事件）
      this.earlyApplicationListeners = new LinkedHashSet<>(this.applicationListeners);
    } else {
      // 将本地应用程序侦听器重置为预刷新状态。
      this.applicationListeners.clear();
      this.applicationListeners.addAll(this.earlyApplicationListeners);
    }

    // 允许收集早期的ApplicationEvents，一旦组播器可用就发布…
    this.earlyApplicationEvents = new LinkedHashSet<>();
  }

  /**
   * Replace any stub property sources with actual instances.
   *
   * @see org.springframework.core.env.PropertySource.StubPropertySource
   * @see org.springframework.web.context.support.WebApplicationContextUtils
   *     #initServletPropertySources
   */
  protected void initPropertySources() {
    // For subclasses: do nothing by default.
  }

  /**
   * 告诉子类刷新内部bean工厂
   *
   * @return 新的BeanFactory实例
   * @see #refreshBeanFactory()
   * @see #getBeanFactory()
   */
  protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
    // 1.初始化beanFactory，并执行加载和解析配置操作
    refreshBeanFactory();
    // 返回beanFactory实例
    return getBeanFactory();
  }

  /**
   * 配置工厂的标准上下文特征，比如上下文的ClassLoader和后置处理器。
   *
   * @param beanFactory 要配置的BeanFactory
   */
  protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    // 告诉内部bean工厂使用上下文的类装入器等。
    beanFactory.setBeanClassLoader(getClassLoader());
    beanFactory.setBeanExpressionResolver(
        new StandardBeanExpressionResolver(beanFactory.getBeanClassLoader()));
    beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this, getEnvironment()));

    // 用上下文回调配置bean工厂。
    beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));
    beanFactory.ignoreDependencyInterface(EnvironmentAware.class);
    beanFactory.ignoreDependencyInterface(EmbeddedValueResolverAware.class);
    beanFactory.ignoreDependencyInterface(ResourceLoaderAware.class);
    beanFactory.ignoreDependencyInterface(ApplicationEventPublisherAware.class);
    beanFactory.ignoreDependencyInterface(MessageSourceAware.class);
    beanFactory.ignoreDependencyInterface(ApplicationContextAware.class);

    // BeanFactory接口未在普通工厂中注册为可解析类型。MessageSource作为bean注册(并自动查找)。
    beanFactory.registerResolvableDependency(BeanFactory.class, beanFactory);
    beanFactory.registerResolvableDependency(ResourceLoader.class, this);
    beanFactory.registerResolvableDependency(ApplicationEventPublisher.class, this);
    beanFactory.registerResolvableDependency(ApplicationContext.class, this);

    // 将检测内部bean的早期后处理器注册为ApplicationListeners。
    beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(this));

    // 检测LoadTimeWeaver并准备编织，如果发现的话
    if (beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
      beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
      // 为类型匹配设置一个临时ClassLoader。
      beanFactory.setTempClassLoader(
          new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
    }

    // 注册默认环境bean。
    if (!beanFactory.containsLocalBean(ENVIRONMENT_BEAN_NAME)) {
      beanFactory.registerSingleton(ENVIRONMENT_BEAN_NAME, getEnvironment());
    }
    if (!beanFactory.containsLocalBean(SYSTEM_PROPERTIES_BEAN_NAME)) {
      beanFactory.registerSingleton(
          SYSTEM_PROPERTIES_BEAN_NAME, getEnvironment().getSystemProperties());
    }
    if (!beanFactory.containsLocalBean(SYSTEM_ENVIRONMENT_BEAN_NAME)) {
      beanFactory.registerSingleton(
          SYSTEM_ENVIRONMENT_BEAN_NAME, getEnvironment().getSystemEnvironment());
    }
  }

  /**
   * 在标准初始化之后修改应用程序上下文的内部bean工厂。所有的bean定义都已加载，但还没有实例化任何bean。这允许在特定的ApplicationContext实现中注册特殊的BeanPostProcessors等。
   *
   * @param beanFactory 应用程序上下文使用的bean工厂
   */
  protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {}

  /**
   * 实例化并调用所有已注册的BeanFactoryPostProcessor bean，如果给定，则遵循显式顺序。
   *
   * <p>必须在单例实例化之前调用。
   */
  protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
    // 调用Bean工厂后置处理器
    PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(
        beanFactory, getBeanFactoryPostProcessors());

    // 如果在此期间发现(例如通过ConfigurationClassPostProcessor注册的@Bean方法)，则检测LoadTimeWeaver并设置，
    if (beanFactory.getTempClassLoader() == null
        && beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
      // 添加一个新的BeanPostProcessor
      beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
      // 指定用于类型匹配目的的临时ClassLoader
      beanFactory.setTempClassLoader(
          new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
    }
  }

  /**
   * 实例化并注册所有BeanPostProcessor bean，如果给定的话，遵循显式顺序。
   *
   * <p>必须在应用程序bean的任何实例化之前调用。
   */
  protected void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory) {
    PostProcessorRegistrationDelegate.registerBeanPostProcessors(beanFactory, this);
  }

  /** 初始化MessageSource。如果在此上下文中没有定义，则使用父类的. */
  protected void initMessageSource() {
    ConfigurableListableBeanFactory beanFactory = getBeanFactory();
    if (beanFactory.containsLocalBean(MESSAGE_SOURCE_BEAN_NAME)) {
      this.messageSource = beanFactory.getBean(MESSAGE_SOURCE_BEAN_NAME, MessageSource.class);
      // Make MessageSource aware of parent MessageSource.
      if (this.parent != null && this.messageSource instanceof HierarchicalMessageSource) {
        HierarchicalMessageSource hms = (HierarchicalMessageSource) this.messageSource;
        if (hms.getParentMessageSource() == null) {
          // Only set parent context as parent MessageSource if no parent MessageSource
          // registered already.
          hms.setParentMessageSource(getInternalParentMessageSource());
        }
      }
      if (logger.isTraceEnabled()) {
        logger.trace("Using MessageSource [" + this.messageSource + "]");
      }
    } else {
      // Use empty MessageSource to be able to accept getMessage calls.
      DelegatingMessageSource dms = new DelegatingMessageSource();
      dms.setParentMessageSource(getInternalParentMessageSource());
      this.messageSource = dms;
      beanFactory.registerSingleton(MESSAGE_SOURCE_BEAN_NAME, this.messageSource);
      if (logger.isTraceEnabled()) {
        logger.trace(
            "No '" + MESSAGE_SOURCE_BEAN_NAME + "' bean, using [" + this.messageSource + "]");
      }
    }
  }

  /**
   * Initialize the ApplicationEventMulticaster. Uses SimpleApplicationEventMulticaster if none
   * defined in the context.
   *
   * @see org.springframework.context.event.SimpleApplicationEventMulticaster
   */
  protected void initApplicationEventMulticaster() {
    ConfigurableListableBeanFactory beanFactory = getBeanFactory();
    if (beanFactory.containsLocalBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)) {
      this.applicationEventMulticaster =
          beanFactory.getBean(
              APPLICATION_EVENT_MULTICASTER_BEAN_NAME, ApplicationEventMulticaster.class);
      if (logger.isTraceEnabled()) {
        logger.trace(
            "Using ApplicationEventMulticaster [" + this.applicationEventMulticaster + "]");
      }
    } else {
      this.applicationEventMulticaster = new SimpleApplicationEventMulticaster(beanFactory);
      beanFactory.registerSingleton(
          APPLICATION_EVENT_MULTICASTER_BEAN_NAME, this.applicationEventMulticaster);
      if (logger.isTraceEnabled()) {
        logger.trace(
            "No '"
                + APPLICATION_EVENT_MULTICASTER_BEAN_NAME
                + "' bean, using "
                + "["
                + this.applicationEventMulticaster.getClass().getSimpleName()
                + "]");
      }
    }
  }

  /**
   * Initialize the LifecycleProcessor. Uses DefaultLifecycleProcessor if none defined in the
   * context.
   *
   * @see org.springframework.context.support.DefaultLifecycleProcessor
   */
  protected void initLifecycleProcessor() {
    ConfigurableListableBeanFactory beanFactory = getBeanFactory();
    if (beanFactory.containsLocalBean(LIFECYCLE_PROCESSOR_BEAN_NAME)) {
      this.lifecycleProcessor =
          beanFactory.getBean(LIFECYCLE_PROCESSOR_BEAN_NAME, LifecycleProcessor.class);
      if (logger.isTraceEnabled()) {
        logger.trace("Using LifecycleProcessor [" + this.lifecycleProcessor + "]");
      }
    } else {
      DefaultLifecycleProcessor defaultProcessor = new DefaultLifecycleProcessor();
      defaultProcessor.setBeanFactory(beanFactory);
      this.lifecycleProcessor = defaultProcessor;
      beanFactory.registerSingleton(LIFECYCLE_PROCESSOR_BEAN_NAME, this.lifecycleProcessor);
      if (logger.isTraceEnabled()) {
        logger.trace(
            "No '"
                + LIFECYCLE_PROCESSOR_BEAN_NAME
                + "' bean, using "
                + "["
                + this.lifecycleProcessor.getClass().getSimpleName()
                + "]");
      }
    }
  }

  /**
   * Template method which can be overridden to add context-specific refresh work. Called on
   * initialization of special beans, before instantiation of singletons.
   *
   * <p>This implementation is empty.
   *
   * @throws BeansException in case of errors
   * @see #refresh()
   */
  protected void onRefresh() throws BeansException {
    // For subclasses: do nothing by default.
  }

  /**
   * Add beans that implement ApplicationListener as listeners. Doesn't affect other listeners,
   * which can be added without being beans.
   */
  protected void registerListeners() {
    // Register statically specified listeners first.
    for (ApplicationListener<?> listener : getApplicationListeners()) {
      getApplicationEventMulticaster().addApplicationListener(listener);
    }

    // Do not initialize FactoryBeans here: We need to leave all regular beans
    // uninitialized to let post-processors apply to them!
    String[] listenerBeanNames = getBeanNamesForType(ApplicationListener.class, true, false);
    for (String listenerBeanName : listenerBeanNames) {
      getApplicationEventMulticaster().addApplicationListenerBean(listenerBeanName);
    }

    // Publish early application events now that we finally have a multicaster...
    Set<ApplicationEvent> earlyEventsToProcess = this.earlyApplicationEvents;
    this.earlyApplicationEvents = null;
    if (!CollectionUtils.isEmpty(earlyEventsToProcess)) {
      for (ApplicationEvent earlyEvent : earlyEventsToProcess) {
        getApplicationEventMulticaster().multicastEvent(earlyEvent);
      }
    }
  }

  /**
   * Finish the initialization of this context's bean factory, initializing all remaining singleton
   * beans.
   */
  protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
    // Initialize conversion service for this context.
    if (beanFactory.containsBean(CONVERSION_SERVICE_BEAN_NAME)
        && beanFactory.isTypeMatch(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class)) {
      beanFactory.setConversionService(
          beanFactory.getBean(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
    }

    // Register a default embedded value resolver if no bean post-processor
    // (such as a PropertyPlaceholderConfigurer bean) registered any before:
    // at this point, primarily for resolution in annotation attribute values.
    if (!beanFactory.hasEmbeddedValueResolver()) {
      beanFactory.addEmbeddedValueResolver(strVal -> getEnvironment().resolvePlaceholders(strVal));
    }

    // Initialize LoadTimeWeaverAware beans early to allow for registering their transformers early.
    String[] weaverAwareNames =
        beanFactory.getBeanNamesForType(LoadTimeWeaverAware.class, false, false);
    for (String weaverAwareName : weaverAwareNames) {
      getBean(weaverAwareName);
    }

    // Stop using the temporary ClassLoader for type matching.
    beanFactory.setTempClassLoader(null);

    // Allow for caching all bean definition metadata, not expecting further changes.
    beanFactory.freezeConfiguration();

    // Instantiate all remaining (non-lazy-init) singletons.
    beanFactory.preInstantiateSingletons();
  }

  /**
   * Finish the refresh of this context, invoking the LifecycleProcessor's onRefresh() method and
   * publishing the {@link org.springframework.context.event.ContextRefreshedEvent}.
   */
  protected void finishRefresh() {
    // Clear context-level resource caches (such as ASM metadata from scanning).
    clearResourceCaches();

    // Initialize lifecycle processor for this context.
    initLifecycleProcessor();

    // Propagate refresh to lifecycle processor first.
    getLifecycleProcessor().onRefresh();

    // Publish the final event.
    publishEvent(new ContextRefreshedEvent(this));

    // Participate in LiveBeansView MBean, if active.
    LiveBeansView.registerApplicationContext(this);
  }

  /**
   * Cancel this context's refresh attempt, resetting the {@code active} flag after an exception got
   * thrown.
   *
   * @param ex the exception that led to the cancellation
   */
  protected void cancelRefresh(BeansException ex) {
    this.active.set(false);
  }

  /**
   * Reset Spring's common reflection metadata caches, in particular the {@link ReflectionUtils},
   * {@link AnnotationUtils}, {@link ResolvableType} and {@link CachedIntrospectionResults} caches.
   *
   * @since 4.2
   * @see ReflectionUtils#clearCache()
   * @see AnnotationUtils#clearCache()
   * @see ResolvableType#clearCache()
   * @see CachedIntrospectionResults#clearClassLoader(ClassLoader)
   */
  protected void resetCommonCaches() {
    ReflectionUtils.clearCache();
    AnnotationUtils.clearCache();
    ResolvableType.clearCache();
    CachedIntrospectionResults.clearClassLoader(getClassLoader());
  }

  /**
   * Register a shutdown hook {@linkplain Thread#getName() named} {@code SpringContextShutdownHook}
   * with the JVM runtime, closing this context on JVM shutdown unless it has already been closed at
   * that time.
   *
   * <p>Delegates to {@code doClose()} for the actual closing procedure.
   *
   * @see Runtime#addShutdownHook
   * @see ConfigurableApplicationContext#SHUTDOWN_HOOK_THREAD_NAME
   * @see #close()
   * @see #doClose()
   */
  @Override
  public void registerShutdownHook() {
    if (this.shutdownHook == null) {
      // No shutdown hook registered yet.
      this.shutdownHook =
          new Thread(SHUTDOWN_HOOK_THREAD_NAME) {
            @Override
            public void run() {
              synchronized (startupShutdownMonitor) {
                doClose();
              }
            }
          };
      Runtime.getRuntime().addShutdownHook(this.shutdownHook);
    }
  }

  /**
   * Callback for destruction of this instance, originally attached to a {@code DisposableBean}
   * implementation (not anymore in 5.0).
   *
   * <p>The {@link #close()} method is the native way to shut down an ApplicationContext, which this
   * method simply delegates to.
   *
   * @deprecated as of Spring Framework 5.0, in favor of {@link #close()}
   */
  @Deprecated
  public void destroy() {
    close();
  }

  /**
   * Close this application context, destroying all beans in its bean factory.
   *
   * <p>Delegates to {@code doClose()} for the actual closing procedure. Also removes a JVM shutdown
   * hook, if registered, as it's not needed anymore.
   *
   * @see #doClose()
   * @see #registerShutdownHook()
   */
  @Override
  public void close() {
    synchronized (this.startupShutdownMonitor) {
      doClose();
      // If we registered a JVM shutdown hook, we don't need it anymore now:
      // We've already explicitly closed the context.
      if (this.shutdownHook != null) {
        try {
          Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
        } catch (IllegalStateException ex) {
          // ignore - VM is already shutting down
        }
      }
    }
  }

  /**
   * Actually performs context closing: publishes a ContextClosedEvent and destroys the singletons
   * in the bean factory of this application context.
   *
   * <p>Called by both {@code close()} and a JVM shutdown hook, if any.
   *
   * @see org.springframework.context.event.ContextClosedEvent
   * @see #destroyBeans()
   * @see #close()
   * @see #registerShutdownHook()
   */
  protected void doClose() {
    // Check whether an actual close attempt is necessary...
    if (this.active.get() && this.closed.compareAndSet(false, true)) {
      if (logger.isDebugEnabled()) {
        logger.debug("Closing " + this);
      }

      LiveBeansView.unregisterApplicationContext(this);

      try {
        // Publish shutdown event.
        publishEvent(new ContextClosedEvent(this));
      } catch (Throwable ex) {
        logger.warn("Exception thrown from ApplicationListener handling ContextClosedEvent", ex);
      }

      // Stop all Lifecycle beans, to avoid delays during individual destruction.
      if (this.lifecycleProcessor != null) {
        try {
          this.lifecycleProcessor.onClose();
        } catch (Throwable ex) {
          logger.warn("Exception thrown from LifecycleProcessor on context close", ex);
        }
      }

      // Destroy all cached singletons in the context's BeanFactory.
      destroyBeans();

      // Close the state of this context itself.
      closeBeanFactory();

      // Let subclasses do some final clean-up if they wish...
      onClose();

      // Reset local application listeners to pre-refresh state.
      if (this.earlyApplicationListeners != null) {
        this.applicationListeners.clear();
        this.applicationListeners.addAll(this.earlyApplicationListeners);
      }

      // Switch to inactive.
      this.active.set(false);
    }
  }

  /**
   * 模板方法，用于销毁此上下文管理的所有bean。默认的实现通过调用{@code
   * DisposableBean.destroy()}和指定的“destroy-method”来销毁上下文中所有缓存的单例。.
   *
   * <p>可以被重写，以便在标准单例销毁之前或之后添加特定于上下文的bean销毁步骤，而上下文的BeanFactory仍然处于活动状态。
   *
   * @see #getBeanFactory()
   * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#destroySingletons()
   */
  protected void destroyBeans() {
    getBeanFactory().destroySingletons();
  }

  /**
   * Template method which can be overridden to add context-specific shutdown work. The default
   * implementation is empty.
   *
   * <p>Called at the end of {@link #doClose}'s shutdown procedure, after this context's BeanFactory
   * has been closed. If custom shutdown logic needs to execute while the BeanFactory is still
   * active, override the {@link #destroyBeans()} method instead.
   */
  protected void onClose() {
    // For subclasses: do nothing by default.
  }

  @Override
  public boolean isActive() {
    return this.active.get();
  }

  /**
   * Assert that this context's BeanFactory is currently active, throwing an {@link
   * IllegalStateException} if it isn't.
   *
   * <p>Invoked by all {@link BeanFactory} delegation methods that depend on an active context, i.e.
   * in particular all bean accessor methods.
   *
   * <p>The default implementation checks the {@link #isActive() 'active'} status of this context
   * overall. May be overridden for more specific checks, or for a no-op if {@link
   * #getBeanFactory()} itself throws an exception in such a case.
   */
  protected void assertBeanFactoryActive() {
    if (!this.active.get()) {
      if (this.closed.get()) {
        throw new IllegalStateException(getDisplayName() + " has been closed already");
      } else {
        throw new IllegalStateException(getDisplayName() + " has not been refreshed yet");
      }
    }
  }

  // ---------------------------------------------------------------------
  // Implementation of BeanFactory interface
  // ---------------------------------------------------------------------

  @Override
  public Object getBean(String name) throws BeansException {
    assertBeanFactoryActive();
    return getBeanFactory().getBean(name);
  }

  @Override
  public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
    assertBeanFactoryActive();
    return getBeanFactory().getBean(name, requiredType);
  }

  @Override
  public Object getBean(String name, Object... args) throws BeansException {
    assertBeanFactoryActive();
    return getBeanFactory().getBean(name, args);
  }

  @Override
  public <T> T getBean(Class<T> requiredType) throws BeansException {
    assertBeanFactoryActive();
    return getBeanFactory().getBean(requiredType);
  }

  @Override
  public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
    assertBeanFactoryActive();
    return getBeanFactory().getBean(requiredType, args);
  }

  @Override
  public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeanProvider(requiredType);
  }

  @Override
  public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeanProvider(requiredType);
  }

  @Override
  public boolean containsBean(String name) {
    return getBeanFactory().containsBean(name);
  }

  @Override
  public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
    assertBeanFactoryActive();
    return getBeanFactory().isSingleton(name);
  }

  @Override
  public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
    assertBeanFactoryActive();
    return getBeanFactory().isPrototype(name);
  }

  @Override
  public boolean isTypeMatch(String name, ResolvableType typeToMatch)
      throws NoSuchBeanDefinitionException {
    assertBeanFactoryActive();
    return getBeanFactory().isTypeMatch(name, typeToMatch);
  }

  @Override
  public boolean isTypeMatch(String name, Class<?> typeToMatch)
      throws NoSuchBeanDefinitionException {
    assertBeanFactoryActive();
    return getBeanFactory().isTypeMatch(name, typeToMatch);
  }

  @Override
  @Nullable
  public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
    assertBeanFactoryActive();
    return getBeanFactory().getType(name);
  }

  @Override
  @Nullable
  public Class<?> getType(String name, boolean allowFactoryBeanInit)
      throws NoSuchBeanDefinitionException {
    assertBeanFactoryActive();
    return getBeanFactory().getType(name, allowFactoryBeanInit);
  }

  @Override
  public String[] getAliases(String name) {
    return getBeanFactory().getAliases(name);
  }

  // ---------------------------------------------------------------------
  // Implementation of ListableBeanFactory interface
  // ---------------------------------------------------------------------

  @Override
  public boolean containsBeanDefinition(String beanName) {
    return getBeanFactory().containsBeanDefinition(beanName);
  }

  @Override
  public int getBeanDefinitionCount() {
    return getBeanFactory().getBeanDefinitionCount();
  }

  @Override
  public String[] getBeanDefinitionNames() {
    return getBeanFactory().getBeanDefinitionNames();
  }

  @Override
  public String[] getBeanNamesForType(ResolvableType type) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeanNamesForType(type);
  }

  @Override
  public String[] getBeanNamesForType(
      ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
  }

  @Override
  public String[] getBeanNamesForType(@Nullable Class<?> type) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeanNamesForType(type);
  }

  @Override
  public String[] getBeanNamesForType(
      @Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
  }

  @Override
  public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type) throws BeansException {
    assertBeanFactoryActive();
    return getBeanFactory().getBeansOfType(type);
  }

  @Override
  public <T> Map<String, T> getBeansOfType(
      @Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
      throws BeansException {

    assertBeanFactoryActive();
    return getBeanFactory().getBeansOfType(type, includeNonSingletons, allowEagerInit);
  }

  @Override
  public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeanNamesForAnnotation(annotationType);
  }

  @Override
  public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType)
      throws BeansException {

    assertBeanFactoryActive();
    return getBeanFactory().getBeansWithAnnotation(annotationType);
  }

  @Override
  @Nullable
  public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
      throws NoSuchBeanDefinitionException {

    assertBeanFactoryActive();
    return getBeanFactory().findAnnotationOnBean(beanName, annotationType);
  }

  // ---------------------------------------------------------------------
  // Implementation of HierarchicalBeanFactory interface
  // ---------------------------------------------------------------------

  @Override
  @Nullable
  public BeanFactory getParentBeanFactory() {
    return getParent();
  }

  @Override
  public boolean containsLocalBean(String name) {
    return getBeanFactory().containsLocalBean(name);
  }

  /**
   * 如果父上下文实现了ConfigurableApplicationContext， 则返回父上下文的内部bean工厂;否则，返回父上下文本身。
   *
   * @see org.springframework.context.ConfigurableApplicationContext#getBeanFactory
   */
  @Nullable
  protected BeanFactory getInternalParentBeanFactory() {
    return (getParent() instanceof ConfigurableApplicationContext
        ? ((ConfigurableApplicationContext) getParent()).getBeanFactory()
        : getParent());
  }

  // ---------------------------------------------------------------------
  // Implementation of MessageSource interface
  // ---------------------------------------------------------------------

  @Override
  public String getMessage(
      String code, @Nullable Object[] args, @Nullable String defaultMessage, Locale locale) {
    return getMessageSource().getMessage(code, args, defaultMessage, locale);
  }

  @Override
  public String getMessage(String code, @Nullable Object[] args, Locale locale)
      throws NoSuchMessageException {
    return getMessageSource().getMessage(code, args, locale);
  }

  @Override
  public String getMessage(MessageSourceResolvable resolvable, Locale locale)
      throws NoSuchMessageException {
    return getMessageSource().getMessage(resolvable, locale);
  }

  /**
   * Return the internal MessageSource used by the context.
   *
   * @return the internal MessageSource (never {@code null})
   * @throws IllegalStateException if the context has not been initialized yet
   */
  private MessageSource getMessageSource() throws IllegalStateException {
    if (this.messageSource == null) {
      throw new IllegalStateException(
          "MessageSource not initialized - "
              + "call 'refresh' before accessing messages via the context: "
              + this);
    }
    return this.messageSource;
  }

  /**
   * Return the internal message source of the parent context if it is an AbstractApplicationContext
   * too; else, return the parent context itself.
   */
  @Nullable
  protected MessageSource getInternalParentMessageSource() {
    return (getParent() instanceof AbstractApplicationContext
        ? ((AbstractApplicationContext) getParent()).messageSource
        : getParent());
  }

  // ---------------------------------------------------------------------
  // Implementation of ResourcePatternResolver interface
  // ---------------------------------------------------------------------

  @Override
  public Resource[] getResources(String locationPattern) throws IOException {
    return this.resourcePatternResolver.getResources(locationPattern);
  }

  // ---------------------------------------------------------------------
  // Implementation of Lifecycle interface
  // ---------------------------------------------------------------------

  @Override
  public void start() {
    getLifecycleProcessor().start();
    publishEvent(new ContextStartedEvent(this));
  }

  @Override
  public void stop() {
    getLifecycleProcessor().stop();
    publishEvent(new ContextStoppedEvent(this));
  }

  @Override
  public boolean isRunning() {
    return (this.lifecycleProcessor != null && this.lifecycleProcessor.isRunning());
  }

  // ---------------------------------------------------------------------
  // 必须由子类实现的抽象方法
  // ---------------------------------------------------------------------

  /**
   * 子类必须实现此方法才能执行实际的配置加载。在任何其他初始化工作之前，该方法由{@link refresh()}调用。
   *
   * <p>子类要么创建一个新的bean工厂并保存对它的引用，要么返回它保存的单个BeanFactory实例。在后一种情况下，
   * 如果多次刷新上下文，通常会抛出一个IllegalStateException。
   *
   * @throws BeansException 如果bean工厂初始化失败抛出此异常
   * @throws IllegalStateException 如果已初始化且多次刷新尝试未初始化抛出此异常 supported
   */
  protected abstract void refreshBeanFactory() throws BeansException, IllegalStateException;

  /**
   * Subclasses must implement this method to release their internal bean factory. This method gets
   * invoked by {@link #close()} after all other shutdown work.
   *
   * <p>Should never throw an exception but rather log shutdown failures.
   */
  protected abstract void closeBeanFactory();

  /**
   * 子类必须在这里返回它们的内部bean工厂。它们应该有效地实现查找，这样就可以重复调用而不会造成性能损失。
   *
   * <p>注意:子类在返回内部bean工厂之前应该检查上下文是否仍然是活动的。一旦上下文关闭，通常应该认为内部工厂不可用。
   *
   * @return 这个应用程序上下文的内部bean工厂(从不{@code null})
   * @throws IllegalStateException if the context does not hold an internal bean factory yet
   *     (usually if {@link #refresh()} has never been called) or if the context has been closed
   *     already
   * @see #refreshBeanFactory()
   * @see #closeBeanFactory()
   */
  @Override
  public abstract ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException;

  /** Return information about this context. */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getDisplayName());
    sb.append(", started on ").append(new Date(getStartupDate()));
    ApplicationContext parent = getParent();
    if (parent != null) {
      sb.append(", parent: ").append(parent.getDisplayName());
    }
    return sb.toString();
  }
}
