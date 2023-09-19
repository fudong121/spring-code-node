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

package org.springframework.context.annotation;

/**
 * 枚举各种作用域代理选项。
 *
 * <p>For a more complete discussion of exactly what a scoped proxy is, see the section of the
 * Spring reference documentation entitled '<em>Scoped beans as dependencies</em>'.
 *
 * @author Mark Fisher
 * @since 2.5
 * @see ScopeMetadata
 */
public enum ScopedProxyMode {

  /** 默认值通常等于{@link NO}，除非在组件扫描指令级别配置了不同的默认值 */
  DEFAULT,

  /**
   * 不要创建作用域代理。
   *
   * <p>此代理模式在用于非单例作用域实例时通常不有用，如果要将其用作依赖项，则应该倾向于使用{@link INTERFACES }或{@link TARGET_CLASS}代理模式。
   */
  NO,

  /** 创建一个JDK动态代理，实现由目标对象的类公开的所有<i>接口。 */
  INTERFACES,

  /** 创建一个基于类的代理(使用CGLIB)。 */
  TARGET_CLASS
}
