/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.server;

import com.confighub.api.server.filters.*;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.Method;

@Provider
public class AuthFeature
    implements DynamicFeature
{
    @Override
    public void configure(final ResourceInfo resourceInfo, final FeatureContext context)
    {
        final Method resourceMethod = resourceInfo.getResourceMethod();

        if (resourceMethod.getAnnotation(AuthenticationNotRequired.class) == null)
            context.register(UserLoggedIn.class);
    }
}