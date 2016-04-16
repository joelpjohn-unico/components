/*
 * Copyright (C) 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.talend.components.api.container;

import java.util.Collection;


/**
 * The interface for things that might be output from a {@link PTransform}.
 */
public interface POutput {

  /**
   * Expands this {@link POutput} into a list of its component output
   * {@link PValue PValues}.
   *
   * <ul>
   *   <li>A {@link PValue} expands to itself.</li>
   *   <li>A tuple or list of {@link PValue PValues} (such as
   *     {@link PCollectionTuple} or {@link PCollectionList})
   *     expands to its component {@code PValue PValues}.</li>
   * </ul>
   *
   * <p>Not intended to be invoked directly by user code.
   */
  public Collection<? extends PValue> expand();

  /**
   * As part of applying the producing {@link PTransform}, finalizes this
   * output to make it ready for being used as an input and for running.
   *
   * <p>This includes ensuring that all {@link PCollection PCollections}
   * have {@link Coder Coders} specified or defaulted.
   *
   * <p>Automatically invoked whenever this {@link POutput} is used
   * as a {@link PInput} to another {@link PTransform}, or if never
   * used as a {@link PInput}, when {@link Pipeline#run}
   * is called, so users do not normally call this explicitly.
   */
  public void finishSpecifyingOutput();
}
