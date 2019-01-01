/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 See AUTHORS file
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.mini2Dx.minibus.exception;

import org.mini2Dx.minibus.pool.MessageDataPool;

/**
 * Thrown when a class is missing a constructor with a single parameter of type
 * {@link MessageDataPool}
 */
public class MissingPooledConstructorException extends RuntimeException {
    private static final long serialVersionUID = -906207654187191332L;

    public MissingPooledConstructorException(final Class<?> clazz) {
	super("Class " + clazz.getName() + " is missing a single-arg constructor that accepts a "
		+ MessageDataPool.class.getName() + " parameter");
    }
}