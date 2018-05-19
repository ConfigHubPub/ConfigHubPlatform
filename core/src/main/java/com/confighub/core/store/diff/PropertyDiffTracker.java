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

package com.confighub.core.store.diff;

import com.confighub.core.repository.Property;
import com.confighub.core.repository.PropertyKey;
import com.confighub.core.store.APersisted;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.*;

public class PropertyDiffTracker
    extends ADiffTracker
{
    private static final Logger log = LogManager.getLogger(PropertyDiffTracker.class);

    @PreRemove
    public void preRemove(APersisted obj)
    {
        obj.revType = APersisted.RevisionType.Delete;
        setSearchKey(((Property)obj).getKey());
    }

    @PrePersist
    public void prePersist(APersisted obj)
    {
        obj.revType = APersisted.RevisionType.Add;
    }

    @PostPersist
    public void postPersist(APersisted obj)
    {
        setSearchKey(((Property)obj).getKey());
    }

    @PostLoad
    public void loadOldData(APersisted obj)
    {
        if (!isTracked() || !(obj instanceof Property))
            return;

        OriginalProperty o = new OriginalProperty();

        Property property = (Property)obj;
        PropertyKey key = property.getPropertyKey();

        o.encrypted = property.isEncrypted();
        o.valueDataType = key.getValueDataType();
        o.value = property.getValue();
        o.contextJson = property.getContextJson();
        o.active = property.isActive();

        obj.revType = APersisted.RevisionType.Modify;
        edits.get().put(obj.getId(), o);
    }

    @PreUpdate
    public void preUpdate(APersisted obj)
    {
        OriginalProperty o = (OriginalProperty)getIfRecorded(obj);
        if (null == o || !(obj instanceof Property))
            return;

        Property property = (Property)obj;
        PropertyKey key = property.getPropertyKey();

        JsonObject json = new JsonObject();

        if (!Utils.equal(o.value, property.getValue()))
        {
            if (o.encrypted)
                json.addProperty("value", o.value);
            else
            {
                switch (o.valueDataType)
                {
                    case Map:

                        try {
                            json.add("value", new Gson().fromJson(o.value, JsonObject.class));
                        } catch (Exception e) {
                            json.addProperty("value", o.value);
                        }
                        break;
                    case List:
                        try
                        {
                            json.add("value", new Gson().fromJson(o.value, JsonArray.class));
                        } catch (Exception e) {
                            json.addProperty("value", o.value);
                        }
                        break;
                    default:
                        json.addProperty("value", o.value);
                        break;
                }
            }
        }

        if (!Utils.equal(o.contextJson, property.getContextJson()))
            json.add("context", new Gson().fromJson(o.contextJson, JsonArray.class));

        if (o.valueDataType != key.valueDataType)
            json.addProperty("vdt", o.valueDataType.name());

        property.diffJson = json.toString();
        setSearchKey(((Property)obj).getKey());
    }

    private static class OriginalProperty
        extends OriginalAPersistent
    {
        PropertyKey.ValueDataType valueDataType;
        boolean encrypted;
        String value;
        String contextJson;
        boolean active;

        @Override
        public String toString()
        {
            return String.format("%s\n%s\n%s\n%s\n%s",
                                 valueDataType.name(),
                                 encrypted,
                                 value,
                                 contextJson,
                                 active ? "true" : "false");
        }
    }
}
