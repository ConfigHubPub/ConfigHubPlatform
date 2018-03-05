/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.core.store.diff;

import com.confighub.core.repository.PropertyKey;
import com.confighub.core.store.APersisted;
import com.confighub.core.utils.Utils;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.*;

public class PropertyKeyDiffTracker
        extends ADiffTracker
{
    private static final Logger log = LogManager.getLogger(PropertyKeyDiffTracker.class);

    @PreRemove
    public void preRemove(APersisted obj)
    {
        obj.revType = APersisted.RevisionType.Delete;
        setSearchKey(((PropertyKey)obj).getKey());
    }

    @PrePersist
    public void prePersist(APersisted obj)
    {
        obj.revType = APersisted.RevisionType.Add;
    }

    @PostPersist
    public void postPersist(APersisted obj)
    {
        setSearchKey(((PropertyKey)obj).getKey());
    }

    @PostLoad
    public void loadOldData(APersisted obj)
    {
        if (!isTracked() || !(obj instanceof PropertyKey))
            return;

        OriginalPropertyKey o = new OriginalPropertyKey();
        PropertyKey propertyKey = (PropertyKey)obj;

        o.key = propertyKey.getKey();
        o.readme = propertyKey.getReadme();
        o.deprecated = propertyKey.isDeprecated();
        o.spName = null == propertyKey.getSecurityProfile() ? null : propertyKey.getSecurityProfile().getName();
        o.valueDataType = propertyKey.valueDataType;
        o.push = propertyKey.isPushValueEnabled();

        obj.revType = APersisted.RevisionType.Modify;
        edits.get().put(obj.getId(), o);
    }

    @PreUpdate
    public void preUpdate(APersisted obj)
    {
        OriginalPropertyKey o = (OriginalPropertyKey)getIfRecorded(obj);
        if (null == o || !(obj instanceof PropertyKey))
            return;

        PropertyKey propertyKey = (PropertyKey)obj;
        JsonObject json = new JsonObject();

        if (!Utils.equal(propertyKey.getKey(), o.key))
        {
            json.addProperty("key", o.key);
            markForNotification();
        }

        if (!Utils.equal(propertyKey.getReadme(), o.readme))
            json.addProperty("readme", o.readme);

        if (propertyKey.isDeprecated() != o.deprecated)
        {
            json.addProperty("deprecated", o.deprecated);
            markForNotification();
        }

        if (propertyKey.isPushValueEnabled() != o.push)
        {
            json.addProperty("pushEnabled", o.push);
            markForNotification();
        }

        String spName = null == propertyKey.getSecurityProfile() ? null : propertyKey.getSecurityProfile().getName();
        if (!Utils.equal(spName, o.spName))
        {
            json.addProperty("spName", null == o.spName ? "" : o.spName);
            markForNotification();
        }

        if (propertyKey.getValueDataType() != o.valueDataType)
            json.addProperty("vdt",
                             null == o.valueDataType ? PropertyKey.ValueDataType.Text.name() : o.valueDataType.name());

        propertyKey.diffJson = json.toString();
        setSearchKey(propertyKey.getKey());
    }

    private static class OriginalPropertyKey
            extends OriginalAPersistent
    {
        String key;
        String readme;
        boolean deprecated;
        String spName;
        boolean push;
        PropertyKey.ValueDataType valueDataType;
    }
}
