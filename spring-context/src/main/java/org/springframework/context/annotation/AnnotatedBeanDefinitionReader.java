/*
 * Copyright 2002-2019 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 方便的适配器，用于编程注册bean类。 Convenient adapter for programmatic registration of bean classes.
 *
 * <p>这是{@link ClassPathBeanDefinitionScanner}的替代方案，应用相同的注释解析，但仅适用于显式注册的类。
 *
 * <p>This is an alternative to {@link ClassPathBeanDefinitionScanner}, applying the same resolution
 * of annotations but for explicitly registered classes only.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 3.0
 * @see AnnotationConfigApplicationContext#register
 */
public class AnnotatedBeanDefinitionReader {

  private final BeanDefinitionRegistry registry;

  private BeanNameGenerator beanNameGenerator = AnnotationBeanNameGenerator.INSTANCE;

  private ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();

  private ConditionEvaluator conditionEvaluator;

  /**
   * Create a new {@code AnnotatedBeanDefinitionReader} for the given registry.
   *
   * <p>If the registry is {@link EnvironmentCapable}, e.g. is an {@code ApplicationContext}, the
   * {@link Environment} will be inherited, otherwise a new {@link StandardEnvironment} will be
   * created and used.
   *
   * @param registry the {@code BeanFactory} to load bean definitions into, in the form of a {@code
   *     BeanDefinitionRegistry}
   * @see #AnnotatedBeanDefinitionReader(BeanDefinitionRegistry, Environment)
   * @see #setEnvironment(Environment)
   */
  public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry) {
    this(registry, getOrCreateEnvironment(registry));
  }

  /**
   * Create a new {@code AnnotatedBeanDefinitionReader} for the given registry, using the given
   * {@link Environment}.
   *
   * @param registry the {@code BeanFactory} to load bean definitions into, in the form of a {@code
   *     BeanDefinitionRegistry}
   * @param environment the {@code Environment} to use when evaluating bean definition profiles.
   * @since 3.1
   */
  public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry, Environment environment) {
    Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
    Assert.notNull(environment, "Environment must not be null");
    this.registry = registry;
    this.conditionEvaluator = new ConditionEvaluator(registry, environment, null);
    AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);
  }

  /** Get the BeanDefinitionRegistry that this reader operates on. */
  public final BeanDefinitionRegistry getRegistry() {
    return this.registry;
  }

  /**
   * Set the {@code Environment} to use when evaluating whether {@link
   * Conditional @Conditional}-annotated component classes should be registered.
   *
   * <p>The default is a {@link StandardEnvironment}.
   *
   * @see #registerBean(Class, String, Class...)
   */
  public void setEnvironment(Environment environment) {
    this.conditionEvaluator = new ConditionEvaluator(this.registry, environment, null);
  }

  /**
   * Set the {@code BeanNameGenerator} to use for detected bean classes.
   *
   * <p>The default is a {@link AnnotationBeanNameGenerator}.
   */
  public void setBeanNameGenerator(@Nullable BeanNameGenerator beanNameGenerator) {
    this.beanNameGenerator =
        (beanNameGenerator != null ? beanNameGenerator : AnnotationBeanNameGenerator.INSTANCE);
  }

  /**
   * Set the {@code ScopeMetadataResolver} to use for registered component classes.
   *
   * <p>The default is an {@link AnnotationScopeMetadataResolver}.
   */
  public void setScopeMetadataResolver(@Nullable ScopeMetadataResolver scopeMetadataResolver) {
    this.scopeMetadataResolver =
        (scopeMetadataResolver != null
            ? scopeMetadataResolver
            : new AnnotationScopeMetadataResolver());
  }

  /**
   * Register one or more component classes to be processed.
   *
   * <p>Calls to {@code register} are idempotent; adding the same component class more than once has
   * no additional effect.
   *
   * @param componentClasses one or more component classes, e.g. {@link
   *     Configuration @Configuration} classes
   */
  public void register(Class<?>... componentClasses) {
    for (Class<?> componentClass : componentClasses) {
      // 注册Bean对象
      registerBean(componentClass);
    }
  }

  /**
   * 从给定的bean类注册一个bean，从类声明中派生它的元数据 来自类声明的注解。
   *
   * @param beanClass 反射过来的bean
   */
  public void registerBean(Class<?> beanClass) {
    // 真正开始注册的方法
    doRegisterBean(beanClass, null, null, null, null);
  }

  /**
   * Register a bean from the given bean class, deriving its metadata from class-declared
   * annotations.
   *
   * @param beanClass the class of the bean
   * @param name an explicit name for the bean (or {@code null} for generating a default bean name)
   * @since 5.2
   */
  public void registerBean(Class<?> beanClass, @Nullable String name) {
    doRegisterBean(beanClass, name, null, null, null);
  }

  /**
   * Register a bean from the given bean class, deriving its metadata from class-declared
   * annotations.
   *
   * @param beanClass the class of the bean
   * @param qualifiers specific qualifier annotations to consider, in addition to qualifiers at the
   *     bean class level
   */
  @SuppressWarnings("unchecked")
  public void registerBean(Class<?> beanClass, Class<? extends Annotation>... qualifiers) {
    doRegisterBean(beanClass, null, qualifiers, null, null);
  }

  /**
   * Register a bean from the given bean class, deriving its metadata from class-declared
   * annotations.
   *
   * @param beanClass the class of the bean
   * @param name an explicit name for the bean (or {@code null} for generating a default bean name)
   * @param qualifiers specific qualifier annotations to consider, in addition to qualifiers at the
   *     bean class level
   */
  @SuppressWarnings("unchecked")
  public void registerBean(
      Class<?> beanClass, @Nullable String name, Class<? extends Annotation>... qualifiers) {

    doRegisterBean(beanClass, name, qualifiers, null, null);
  }

  /**
   * Register a bean from the given bean class, deriving its metadata from class-declared
   * annotations, using the given supplier for obtaining a new instance (possibly declared as a
   * lambda expression or method reference).
   *
   * @param beanClass the class of the bean
   * @param supplier a callback for creating an instance of the bean (may be {@code null})
   * @since 5.0
   */
  public <T> void registerBean(Class<T> beanClass, @Nullable Supplier<T> supplier) {
    doRegisterBean(beanClass, null, null, supplier, null);
  }

  /**
   * Register a bean from the given bean class, deriving its metadata from class-declared
   * annotations, using the given supplier for obtaining a new instance (possibly declared as a
   * lambda expression or method reference).
   *
   * @param beanClass the class of the bean
   * @param name an explicit name for the bean (or {@code null} for generating a default bean name)
   * @param supplier a callback for creating an instance of the bean (may be {@code null})
   * @since 5.0
   */
  public <T> void registerBean(
      Class<T> beanClass, @Nullable String name, @Nullable Supplier<T> supplier) {
    doRegisterBean(beanClass, name, null, supplier, null);
  }

  /**
   * Register a bean from the given bean class, deriving its metadata from class-declared
   * annotations.
   *
   * @param beanClass the class of the bean
   * @param name an explicit name for the bean (or {@code null} for generating a default bean name)
   * @param supplier a callback for creating an instance of the bean (may be {@code null})
   * @param customizers one or more callbacks for customizing the factory's {@link BeanDefinition},
   *     e.g. setting a lazy-init or primary flag
   * @since 5.2
   */
  public <T> void registerBean(
      Class<T> beanClass,
      @Nullable String name,
      @Nullable Supplier<T> supplier,
      BeanDefinitionCustomizer... customizers) {

    doRegisterBean(beanClass, name, null, supplier, customizers);
  }

  /**
   * Register a bean from the given bean class, deriving its metadata from class-declared
   * annotations.
   *
   * @param beanClass the class of the bean
   * @param name an explicit name for the bean
   * @param qualifiers specific qualifier annotations to consider, if any, in addition to qualifiers
   *     at the bean class level
   * @param supplier a callback for creating an instance of the bean (may be {@code null})
   * @param customizers one or more callbacks for customizing the factory's {@link BeanDefinition},
   *     e.g. setting a lazy-init or primary flag
   * @since 5.0
   */
  private <T> void doRegisterBean(
      Class<T> beanClass,
      @Nullable String name,
      @Nullable Class<? extends Annotation>[] qualifiers,
      @Nullable Supplier<T> supplier,
      @Nullable BeanDefinitionCustomizer[] customizers) {

    AnnotatedGenericBeanDefinition abd = new AnnotatedGenericBeanDefinition(beanClass);
    // 判断是否需要跳过此类（判断abd.getMetadata()上是否有忽略的注解）
    if (this.conditionEvaluator.shouldSkip(abd.getMetadata())) {
      return;
    }
    // 指定用于创建bean实例的回调，作为声明式指定的工厂方法的替代方法。
    abd.setInstanceSupplier(supplier);
    // 用于解析bean定义的作用域
    ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(abd);
    // 设置作用域名称（默认为singleton 单例  ）
    abd.setScope(scopeMetadata.getScopeName());
    // bean名称
    String beanName =
        (name != null ? name : this.beanNameGenerator.generateBeanName(abd, this.registry));
    // 判断通用限定符
    AnnotationConfigUtils.processCommonDefinitionAnnotations(abd);
    if (qualifiers != null) {
      for (Class<? extends Annotation> qualifier : qualifiers) {
        if (Primary.class == qualifier) {
          abd.setPrimary(true);
        } else if (Lazy.class == qualifier) {
          abd.setLazyInit(true);
        } else {
          abd.addQualifier(new AutowireCandidateQualifier(qualifier));
        }
      }
    }
    if (customizers != null) {
      for (BeanDefinitionCustomizer customizer : customizers) {
        customizer.customize(abd);
      }
    }
    // 带有名称和别名的BeanDefinition的Holder。可以注册为内部bean的占位符。
    BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(abd, beanName);
    // 作用域代理模式
    definitionHolder =
        AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
    //  向给定的bean工厂注册给定的bean定义.(核心注册方法)
    BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, this.registry);
  }

  /** 如果可能的话，从给定的注册中心获取环境，否则返回一个新的StandardEnvironment。 */
  private static Environment getOrCreateEnvironment(BeanDefinitionRegistry registry) {
    Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
    if (registry instanceof EnvironmentCapable) {
      return ((EnvironmentCapable) registry).getEnvironment();
    }
    return new StandardEnvironment();
  }
}