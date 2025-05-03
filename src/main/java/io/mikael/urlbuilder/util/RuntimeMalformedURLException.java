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

/**
 * Runtime exception wrapper for {@link java.net.MalformedURLException}.
 * <p>
 * This exception is used to convert checked {@code MalformedURLException}s
 * into unchecked exceptions.
 * </p>
 *
 * @author Mikael Gueck {@literal <gumi@iki.fi>}
 */
public class RuntimeMalformedURLException extends RuntimeException {

    public RuntimeMalformedURLException(final Throwable cause) {
        super(cause);
    }

    /**
     * Returns {@code null} to suppress the stack trace.
     * <p>
     * This implementation is optimized for performance by avoiding the
     * expensive stack trace collection process.
     * </p>
     *
     * @return always {@code null}
     */
    @Override
    public Throwable fillInStackTrace() {
        return null;
    }

}
