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

package org.springframework.beans.factory.support;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.AliasRegistry;

/**
 * 用于保存bean定义的注册中心的接口，例如RootBeanDefinition和ChildBeanDefinition实例。通常由内部使用AbstractBeanDefinition层次结构的BeanFactories实现。
 *
 * <p>这是Spring的bean工厂包中封装bean定义的<i>注册<i>的唯一接口。标准BeanFactory接口只支持对完全配置的工厂实例<i>的访问。
 *
 * <p>Spring的bean定义读取器期望处理该接口的实现。Spring核心中的已知实现者是DefaultListableBeanFactory和GenericApplicationContext。
 *
 * @author Juergen Hoeller
 * @since 26.11.2003
 * @see org.springframework.beans.factory.config.BeanDefinition
 * @see AbstractBeanDefinition
 * @see RootBeanDefinition
 * @see ChildBeanDefinition
 * @see DefaultListableBeanFactory
 * @see org.springframework.context.support.GenericApplicationContext
 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
 * @see PropertiesBeanDefinitionReader
 */
public interface BeanDefinitionRegistry extends AliasRegistry {

  /**
   * 用这个注册中心注册一个新的bean定义。 必须支持RootBeanDefinition和ChildBeanDefinition。
   *
   * @param beanName 要注册的bean实例的名称
   * @param beanDefinition 要注册的bean实例的定义
   * @throws BeanDefinitionStoreException if the BeanDefinition is invalid
   * @throws BeanDefinitionOverrideException if there is already a BeanDefinition 指定的bean名称，并且不允许重写它
   * @see GenericBeanDefinition
   * @see RootBeanDefinition
   * @see ChildBeanDefinition
   */
  void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
      throws BeanDefinitionStoreException;

  /**
   * Remove the BeanDefinition for the given name.
   *
   * @param beanName the name of the bean instance to register
   * @throws NoSuchBeanDefinitionException if there is no such bean definition
   */
  void removeBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

  /**
   * Return the BeanDefinition for the given bean name.
   *
   * @param beanName name of the bean to find a definition for
   * @return the BeanDefinition for the given name (never {@code null})
   * @throws NoSuchBeanDefinitionException if there is no such bean definition
   */
  BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

  /**
   * Check if this registry contains a bean definition with the given name.
   *
   * @param beanName the name of the bean to look for
   * @return if this registry contains a bean definition with the given name
   */
  boolean containsBeanDefinition(String beanName);

  /**
   * Return the names of all beans defined in this registry.
   *
   * @return the names of all beans defined in this registry, or an empty array if none defined
   */
  String[] getBeanDefinitionNames();

  /**
   * Return the number of beans defined in the registry.
   *
   * @return the number of beans defined in the registry
   */
  int getBeanDefinitionCount();

  /**
   * Determine whether the given bean name is already in use within this registry, i.e. whether
   * there is a local bean or alias registered under this name.
   *
   * @param beanName the name to check
   * @return whether the given bean name is already in use
   */
  boolean isBeanNameInUse(String beanName);
}
