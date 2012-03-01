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
package gumi.builders.url;

/**
 * A runtime exception for wrapping java.net.URISyntaxException.
 *
 * @author Mikael Gueck gumi{@literal @}iki.fi
 */
public class RuntimeURISyntaxException extends RuntimeException {

    public RuntimeURISyntaxException(final Throwable cause) {
        super(cause);
    }

    /**
     * We're not interested in the wrapper's stack trace.
     * @return null
     */
    @Override
    public Throwable fillInStackTrace() {
        return null;
    }

}
