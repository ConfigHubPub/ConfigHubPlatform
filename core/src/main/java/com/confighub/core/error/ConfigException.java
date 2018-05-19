/*
 * This file is part of ConfigHub.
 *
 * ConfigHub is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ConfigHub is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ConfigHub.  If not, see <http://www.gnu.org/licenses/>.
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
