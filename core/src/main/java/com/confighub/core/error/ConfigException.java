/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.core.error;

import com.google.gson.JsonObject;

public class ConfigException
    extends RuntimeException
{
    private Error.Code code;
    private JsonObject json;

    public ConfigException(String message)
    {
        super(message);
    }

    public ConfigException(Error.Code code)
    {
        super(code.getMessage());
        this.code = code;
    }

    public ConfigException(Error.Code code,
                           JsonObject json)
    {
        super(code.getMessage());
        this.code = code;
        this.json = json;
    }

    public Error.Code getErrorCode()
    {
        return this.code;
    }

    public JsonObject getJson() { return this.json; }
}
