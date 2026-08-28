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



import java.util.function.Function;
import java.util.function.Supplier;

/** Utility methods for dealing with suppliers. */
public class SupplierUtils {

  private SupplierUtils() {
    // Prevent instantiation
  }

  /**
   * Returns a supplier that memoizes the result returned by another supplier.
   *
   * @param supplier the original supplier
   * @param <T> the type of element returned by the supplier
   * @return the memoized supplier
   */
  public static <T> Supplier<T> memoize(Supplier<T> supplier) {
    T value = supplier.get();
    return () -> value;
  }

  /**
   * Returns a supplier that memoizes the result returned by another supplier for a user defined
   * time to live.
   *
   * @param supplier the original supplier
   * @param timeToLiveMillis the time to live in milliseconds
   * @param <T> the type of element returned by the supplier
   * @return the memoized supplier
   */
  public static <T> Supplier<T> memoize(Supplier<T> supplier, int timeToLiveMillis) {
    return new Supplier<T>() {
      long t1 = System.currentTimeMillis();
      T value = supplier.get();

      @Override
      public T get() {
        long t2 = System.currentTimeMillis();
        if (t2 - t1 > timeToLiveMillis) {
          t1 = t2;
          value = supplier.get();
        }
        return value;
      }
    };
  }

  /**
   * Converts a supplier to another supplier by applying a function.
   *
   * @param supplier the original supplier
   * @param function the function to apply
   * @param <T> the type of elements returned by the original supplier
   * @param <R> the type of elements returned by the function
   * @return the resulting supplier
   */
  public static <T, R> Supplier<R> convert(Supplier<T> supplier, Function<T, R> function) {
    return () -> function.apply(supplier.get());
  }
}
