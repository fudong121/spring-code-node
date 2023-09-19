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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.lang.Nullable;

/**
 * {@link org.springframework.context 的基类。ApplicationContext}实现应该支持对{@link
 * refresh()}的多次调用，每次创建一个新的内部bean工厂实例。通常(但不一定)，这样的上下文将由一组配置位置驱动，以便从中加载bean定义
 *
 * <p>子类实现的唯一方法是{@link loadBeanDefinitions}，它在每次刷新时被调用。具体的实现应该是将bean定义加载到给定的{@link
 * org.springframework.beans.factory.support 中。DefaultListableBeanFactory}，通常委托给一个或多个特定的bean定义读取器。
 *
 * <p><b>注意，WebApplicationContexts有一个类似的基类。<b> {@link
 * org.springframework.web.context.support。trefreshablewebapplicationcontext}提供了相同的子类化策略，但额外地预先实现了web环境的所有上下文功能。还有一种预定义的方式来接收web上下文的配置位置。
 *
 * <p>该基类的具体独立子类，以特定的bean定义格式读取，是{@link ClassPathXmlApplicationContext}和{@link
 * FileSystemXmlApplicationContext}，它们都派生自公共的{@link AbstractXmlApplicationContext}基类;{@link
 * org.springframework.context.annotation。注解configapplicationcontext}支持{@code @Configuration}注解类作为bean定义的来源。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 1.1.3
 * @see #loadBeanDefinitions
 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory
 * @see org.springframework.web.context.support.AbstractRefreshableWebApplicationContext
 * @see AbstractXmlApplicationContext
 * @see ClassPathXmlApplicationContext
 * @see FileSystemXmlApplicationContext
 * @see org.springframework.context.annotation.AnnotationConfigApplicationContext
 */
public abstract class AbstractRefreshableApplicationContext extends AbstractApplicationContext {

  @Nullable private Boolean allowBeanDefinitionOverriding;

  @Nullable private Boolean allowCircularReferences;

  /** 此上下文的Bean工厂 */
  @Nullable private volatile DefaultListableBeanFactory beanFactory;

  /** 创建一个新的AbstractRefreshableApplicationContext，没有父类. */
  public AbstractRefreshableApplicationContext() {}

  /**
   * 用给定的父上下文创建一个新的AbstractRefreshableApplicationContext.
   *
   * @param parent the parent context
   */
  public AbstractRefreshableApplicationContext(@Nullable ApplicationContext parent) {
    super(parent);
  }

  /**
   * 设置是否允许通过注册具有相同名称的不同定义来覆盖bean定义，并自动替换前者。否则，将抛出异常。默认为“true”。
   *
   * @see
   *     org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
   */
  public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
    this.allowBeanDefinitionOverriding = allowBeanDefinitionOverriding;
  }

  /**
   * 设置是否允许bean之间的循环引用—并自动尝试解析它们。
   *
   * <p>默认为"true"。在遇到循环引用时，将此关闭以抛出异常，完全禁止循环引用。
   *
   * @see
   *     org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowCircularReferences
   */
  public void setAllowCircularReferences(boolean allowCircularReferences) {
    this.allowCircularReferences = allowCircularReferences;
  }

  /** 该实现对上下文的底层bean工厂执行实际刷新，关闭上一个bean工厂(如果有的话)，并为上下文生命周期的下一阶段初始化一个新的bean工厂。 */
  @Override
  protected final void refreshBeanFactory() throws BeansException {
    // 判断此上下文当前是否包含bean工厂
    if (hasBeanFactory()) {
      // 用于销毁此上下文管理的所有bean
      destroyBeans();
      // 关闭bean工厂
      closeBeanFactory();
    }
    try {
      // 创建bean工厂
      DefaultListableBeanFactory beanFactory = createBeanFactory();
      // 设置序列化id
      beanFactory.setSerializationId(getId());
      // 自定义bean工厂
      customizeBeanFactory(beanFactory);
      // 将bean定义加载到给定的bean工厂中，通常是通过委托给一个或多个bean定义读取器。
      loadBeanDefinitions(beanFactory);
      // 将配置好的bean工厂赋给应用于上下文的bean工厂（刷新）
      this.beanFactory = beanFactory;
    } catch (IOException ex) {
      throw new ApplicationContextException(
          "I/O error parsing bean definition source for " + getDisplayName(), ex);
    }
  }

  @Override
  protected void cancelRefresh(BeansException ex) {
    DefaultListableBeanFactory beanFactory = this.beanFactory;
    if (beanFactory != null) {
      beanFactory.setSerializationId(null);
    }
    super.cancelRefresh(ex);
  }

  @Override
  protected final void closeBeanFactory() {
    DefaultListableBeanFactory beanFactory = this.beanFactory;
    if (beanFactory != null) {
      beanFactory.setSerializationId(null);
      this.beanFactory = null;
    }
  }

  /** 判断此上下文当前是否包含bean工厂，即至少刷新过一次并且尚未关闭。 */
  protected final boolean hasBeanFactory() {
    return (this.beanFactory != null);
  }

  @Override
  public final ConfigurableListableBeanFactory getBeanFactory() {
    DefaultListableBeanFactory beanFactory = this.beanFactory;
    if (beanFactory == null) {
      throw new IllegalStateException(
          "BeanFactory not initialized or already closed - "
              + "call 'refresh' before accessing beans via the ApplicationContext");
    }
    return beanFactory;
  }

  /**
   * Overridden to turn it into a no-op: With AbstractRefreshableApplicationContext, {@link
   * #getBeanFactory()} serves a strong assertion for an active context anyway.
   */
  @Override
  protected void assertBeanFactoryActive() {}

  /**
   * Create an internal bean factory for this context. Called for each {@link #refresh()} attempt.
   *
   * <p>The default implementation creates a {@link
   * org.springframework.beans.factory.support.DefaultListableBeanFactory} with the {@linkplain
   * #getInternalParentBeanFactory() internal bean factory} of this context's parent as parent bean
   * factory. Can be overridden in subclasses, for example to customize DefaultListableBeanFactory's
   * settings.
   *
   * @return the bean factory for this context
   * @see
   *     org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
   * @see
   *     org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowEagerClassLoading
   * @see
   *     org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowCircularReferences
   * @see
   *     org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowRawInjectionDespiteWrapping
   */
  protected DefaultListableBeanFactory createBeanFactory() {
    return new DefaultListableBeanFactory(getInternalParentBeanFactory());
  }

  /**
   * 自定义此上下文使用的内部bean工厂。每次尝试{@link refresh()}时调用
   *
   * <p><p默认的实现应用这个上下文的{@linkplain setallowbeandefinitionoverride " allowbeandefinitionoverride
   * "}和{@linkplain setAllowCircularReferences
   * "allowCircularReferences"}设置，如果指定的话。可以在子类中重写以自定义{@link DefaultListableBeanFactory}的任何设置。
   *
   * @param beanFactory 为此上下文新创建的bean工厂
   * @see DefaultListableBeanFactory# 设置允许Bean定义重写
   * @see DefaultListableBeanFactory# 设置允许循环引用
   * @see DefaultListableBeanFactory# 设置允许未经包装的注册
   * @see DefaultListableBeanFactory# 设置允许即时类加载
   */
  protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
    if (this.allowBeanDefinitionOverriding != null) {
      beanFactory.setAllowBeanDefinitionOverriding(this.allowBeanDefinitionOverriding);
    }
    if (this.allowCircularReferences != null) {
      beanFactory.setAllowCircularReferences(this.allowCircularReferences);
    }
  }

  /**
   * 将bean定义加载到给定的bean工厂中，通常是通过委托给一个或多个bean定义读取器。
   *
   * @param beanFactory the bean factory to load bean definitions into
   * @throws BeansException if parsing of the bean definitions failed
   * @throws IOException if loading of bean definition files failed
   * @see org.springframework.beans.factory.support.PropertiesBeanDefinitionReader
   * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
   */
  protected abstract void loadBeanDefinitions(DefaultListableBeanFactory beanFactory)
      throws BeansException, IOException;
}
