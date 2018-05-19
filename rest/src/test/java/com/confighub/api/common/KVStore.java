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

package com.confighub.api.common;

import com.confighub.api.repository.user.editor.EditorResolver;
import com.confighub.api.repository.user.property.SaveProperty;
import com.confighub.api.repository.user.property.UpdateKey;
import com.confighub.core.repository.PropertyKey;

import javax.ws.rs.core.Response;

/**
 *
 */
public class KVStore
{
    public static Response getUIConfig(final String account,
                                       final String repositoryName,
                                       final String contextString,
                                       final Long ts,
                                       final boolean allKeys,
                                       final String tagLabel,
                                       final boolean literal,
                                       final String userToken)
    {
        EditorResolver uiResolver = new EditorResolver();
        return uiResolver.get(account, repositoryName, contextString, ts, allKeys, tagLabel, literal, userToken);
    }

    public static Response addOrUpdateProperty(final String account,
                                               final String repositoryName,
                                               final String userToken,
                                               final String key,
                                               final String vdt,
                                               final String value,
                                               final String propertyContext,
                                               final Long propertyId)
    {
        return addOrUpdateProperty(account,
                                   repositoryName,
                                   userToken,
                                   key,
                                   "",
                                   false,
                                   vdt,
                                   false,
                                   value,
                                   propertyContext,
                                   null,
                                   true,
                                   null,
                                   null,
                                   propertyId);
    }

    public static Response addOrUpdateProperty(final String account,
                                               final String repositoryName,
                                               final String userToken,
                                               final String key,
                                               final String value,
                                               final String propertyContext)
    {
        return addOrUpdateProperty(account,
                                   repositoryName,
                                   userToken,
                                   key,
                                   "",
                                   false,
                                   PropertyKey.ValueDataType.Text.name(),
                                   false,
                                   value,
                                   propertyContext,
                                   null,
                                   true,
                                   null,
                                   null,
                                   null);
    }


    public static Response addOrUpdateProperty(final String account,
                                               final String repositoryName,
                                               final String userToken,
                                               final String key,
                                               final String comment,
                                               final boolean deprecated,
                                               final String vdt,
                                               final boolean pushEnabled,
                                               final String value,
                                               final String propertyContext,
                                               final String changeComment,
                                               final boolean active,
                                               final String spPassword,
                                               final String spName,
                                               final Long propertyId)
    {
        SaveProperty propertyAPI = new SaveProperty();
        return propertyAPI.update(account,
                                  repositoryName,
                                  userToken,
                                  key,
                                  comment,
                                  deprecated,
                                  vdt,
                                  pushEnabled,
                                  value,
                                  propertyContext,
                                  changeComment,
                                  active,
                                  spPassword,
                                  spName,
                                  propertyId);
    }

    public static Response updateKey(String account,
                                     String repositoryName,
                                     String originalKey,
                                     String userToken,
                                     String key,
                                     String vdt,
                                     boolean deprecated,
                                     String comment,
                                     String changeComment,
                                     String spName,
                                     String newSpPassword,
                                     String currentPassword,
                                     boolean pushEnabled)
    {
        UpdateKey updateKeyAPI = new UpdateKey();
        return updateKeyAPI.update(account,
                                   repositoryName,
                                   originalKey,
                                   userToken,
                                   key,
                                   vdt,
                                   deprecated,
                                   comment,
                                   changeComment,
                                   spName,
                                   newSpPassword,
                                   currentPassword,
                                   pushEnabled);
    }
}
