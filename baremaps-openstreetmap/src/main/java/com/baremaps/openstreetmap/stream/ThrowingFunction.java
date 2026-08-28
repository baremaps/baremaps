/*
 * Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baremaps.openstreetmap.stream;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.function.Function;

/**
 * Represents a function that accepts one argument, produces a result or throws an exception
 *
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 * @param <E> the type of the exception thrown by the function
 */
@FunctionalInterface
public interface ThrowingFunction<T, R, E extends Exception> {

  /**
   * Applies this function to the given argument.
   *
   * @param t the function argument
   * @return the function result
   * @throws E an exception
   */
  R apply(T t) throws E;

  /**
   * Converts a {@code ThrowingFunction} into a {@code Function} that returns {@code Optional}
   * elements which are empty in case of {@code Exception}.
   *
   * @param throwingFunction the throwing function
   * @param <T> the type of the input to the function
   * @param <R> the type of the result of the function
   * @return the resulting function
   */
  static <T, R> Function<T, Optional<R>> optional(
      final ThrowingFunction<? super T, ? extends R, ?> throwingFunction) {
    requireNonNull(throwingFunction);
    return t -> {
      try {
        return Optional.ofNullable(throwingFunction.apply(t));
      } catch (final Exception e) {
        return Optional.empty();
      }
    };
  }

  /**
   * Converts a {@code ThrowingFunction} into a {@code Function} that returns elements or throws
   * unchecked exceptions in case of {@code Exception}.
   *
   * @param throwingFunction the throwing function
   * @param <T> the type of the input to the function
   * @param <R> the type of the result of the function
   * @return the resulting function
   */
  static <T, R> Function<T, R> unchecked(
      final ThrowingFunction<? super T, ? extends R, ?> throwingFunction) {
    requireNonNull(throwingFunction);
    return t -> {
      try {
        return throwingFunction.apply(t);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    };
  }
}
