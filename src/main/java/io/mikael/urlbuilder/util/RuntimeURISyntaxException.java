/*
Copyright 2012 Mikael Gueck

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package io.mikael.urlbuilder.util;

import java.util.Objects;

/**
 * Unchecked exception wrapper for {@link java.net.URISyntaxException}.
 * <p>
 * This exception converts checked {@code URISyntaxException}s into unchecked exceptions
 * for cleaner error handling in cases where URI syntax validation failures
 * should be treated as unrecoverable errors.
 * </p>
 *
 * @author Mikael Gueck {@literal <gumi@iki.fi>}
 */
public class RuntimeURISyntaxException extends RuntimeException {

    /**
     * Constructs a new runtime URI syntax exception with the specified cause.
     *
     * @param cause the underlying {@code URISyntaxException} (must not be {@code null})
     * @throws NullPointerException if the cause is {@code null}
     */
    public RuntimeURISyntaxException(final Throwable cause) {
        super(Objects.requireNonNull(cause, "Cause must not be null"));
    }

    /**
     * Suppresses stack trace generation for performance optimization.
     * <p>
     * Since this is a wrapper exception, the original exception's stack trace
     * provides sufficient debugging information.
     * </p>
     *
     * @return always {@code null}
     */
    @Override
    public Throwable fillInStackTrace() {
        return null;
    }
}
