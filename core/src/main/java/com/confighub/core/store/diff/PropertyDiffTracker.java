/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
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
