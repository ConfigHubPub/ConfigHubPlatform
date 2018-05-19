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

package com.confighub.api.util;

import com.confighub.core.repository.Property;
import com.confighub.core.repository.PropertyKey;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

/**
 *
 */
public abstract class FileHelper
{

    public enum FileOutputFormatType
    {
        Text,
        JSON_Array,
        JSON_Simple_Map,
        JSON_Map
    }

    public static final String newline = "\r\n";
    public static final String newLineFlat = " ";

    /**
     *
     * @param properties
     * @param includeComments
     * @return
     */
    public static JsonArray getJsonArray(final List<Property> properties,
                                         final boolean includeComments)
    {
        JsonArray arr = new JsonArray();

        for (Property property : properties)
        {
            JsonObject p = new JsonObject();

            PropertyKey key = property.getPropertyKey();
            if (key.isDeprecated())
                p.addProperty("deprecated", true);
            if (key.isEncrypted())
                p.addProperty("encryption", key.getSecurityProfile().getName());

            if (includeComments && !Utils.isBlank(property.getReadme()))
                p.addProperty("comment", Utils.jsonString(property.getReadme()));
            p.addProperty("key", property.getKey());

            if (PropertyKey.ValueDataType.List.equals(key.getValueDataType()))
            {
                JsonArray r = new Gson().fromJson(property.getValue(), JsonArray.class);
                p.add("value", r);
            }
            else
                p.addProperty("value", property.getValue());

            arr.add(p);
        }

        return arr;
    }

    /**
     *
     * @param properties
     * @param includeComments
     * @return
     */
    public static JsonObject getJsonMap(final List<Property> properties,
                                        final boolean includeComments)
    {
        JsonObject json = new JsonObject();
        for (Property property : properties)
        {
            JsonObject p = new JsonObject();

            PropertyKey key = property.getPropertyKey();
            if (key.isDeprecated())
                p.addProperty("deprecated", true);
            if (key.isEncrypted())
                p.addProperty("encryption", key.getSecurityProfile().getName());

            if (includeComments && !Utils.isBlank(property.getReadme()))
                p.addProperty("comment", Utils.jsonString(property.getReadme()));

            if (PropertyKey.ValueDataType.List.equals(key.getValueDataType()))
            {
                JsonArray r = new Gson().fromJson(property.getValue(), JsonArray.class);
                p.add("value", r);
            }
            else
                p.addProperty("value", property.getValue());

            json.add(property.getKey(), p);
        }

        return json;
    }

    /**
     *
     * @param properties
     * @param includeComments
     * @return
     */
    public static JsonObject getJsonMapSimple(final List<Property> properties,
                                              final boolean includeComments)
    {
        if (includeComments)
            return getJsonMap(properties, includeComments);

        JsonObject json = new JsonObject();
        for (Property property : properties)
            json.addProperty(property.getKey(), property.getValue());

        return json;
    }

    /**
     *
     * @param properties
     * @param includeComments
     * @return
     */
    public static String getPlain(final List<Property> properties,
                                  final boolean includeComments)
    {
        StringBuilder sb = new StringBuilder();

        for (Property property : properties)
        {
            if (includeComments)
            {
                String readme = property.getReadme();
                if (!Utils.isBlank(readme))
                {
                    String[] lines = readme.split(System.lineSeparator());

                    for (String line : lines)
                        sb.append("# ").append(line.trim()).append(newline);
                }
            }

            PropertyKey key = property.getPropertyKey();
            if (key.isDeprecated())
                sb.append(";; deprecated").append(newline);
            if (key.isEncrypted())
                sb.append(";; encryption = ").append(key.getSecurityProfile().getName()).append(newline);


            sb.append(property.getKey());
            sb.append(" = ");
            sb.append(property.getValue().replaceAll("\n", newLineFlat).replaceAll("\r", newLineFlat));

            sb.append(newline).append(newline);
        }

        return sb.toString();
    }


}
