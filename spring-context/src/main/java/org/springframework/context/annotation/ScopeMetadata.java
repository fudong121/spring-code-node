/*
 * Copyright 2002-2012 the original author or authors.
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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.util.Assert;

/**
 * 描述spring管理bean的作用域特征，包括作用域名称和作用域代理行为
 *
 * <p>默认作用域是"singleton"，默认是<i>不<i>创建作用域代理。
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 2.5
 * @see ScopeMetadataResolver
 * @see ScopedProxyMode
 */
public class ScopeMetadata {

  private String scopeName = BeanDefinition.SCOPE_SINGLETON;

  private ScopedProxyMode scopedProxyMode = ScopedProxyMode.NO;

  /** Set the name of the scope. */
  public void setScopeName(String scopeName) {
    Assert.notNull(scopeName, "'scopeName' must not be null");
    this.scopeName = scopeName;
  }

  /** Get the name of the scope. */
  public String getScopeName() {
    return this.scopeName;
  }

  /** Set the proxy-mode to be applied to the scoped instance. */
  public void setScopedProxyMode(ScopedProxyMode scopedProxyMode) {
    Assert.notNull(scopedProxyMode, "'scopedProxyMode' must not be null");
    this.scopedProxyMode = scopedProxyMode;
  }

  /** Get the proxy-mode to be applied to the scoped instance. */
  public ScopedProxyMode getScopedProxyMode() {
    return this.scopedProxyMode;
  }
}
