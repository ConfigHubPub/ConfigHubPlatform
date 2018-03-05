/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.core.utils;

import com.confighub.core.error.ConfigException;

@FunctionalInterface
public interface CheckedFunction<T, R> {
    R apply(T t) throws ConfigException;
}