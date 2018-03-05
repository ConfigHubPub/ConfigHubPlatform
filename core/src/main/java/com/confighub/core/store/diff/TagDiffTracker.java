/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.core.store.diff;

import com.confighub.core.repository.Tag;
import com.confighub.core.store.APersisted;
import com.confighub.core.utils.Utils;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.*;

public class TagDiffTracker
        extends ADiffTracker
{
    private static final Logger log = LogManager.getLogger(TagDiffTracker.class);

    @PreRemove
    public void preRemove(APersisted obj)
    {
        obj.revType = APersisted.RevisionType.Delete;
        markForNotification();
    }

    @PrePersist
    public void prePersist(APersisted obj)
    {
        obj.revType = APersisted.RevisionType.Add;
    }

    @PostPersist
    public void postPersist(APersisted obj)
    {
        markForNotification();
    }

    @PostLoad
    public void loadOldData(APersisted obj)
    {
        if (!isTracked() || !(obj instanceof Tag))
            return;

        OriginalTag o = new OriginalTag();
        Tag tag = (Tag) obj;

        o.name = tag.getName();
        o.readme = tag.getReadme();
        o.ts = tag.getTs();

        obj.revType = APersisted.RevisionType.Modify;
        edits.get().put(obj.getId(), o);
    }

    @PreUpdate
    public void preUpdate(APersisted obj)
    {
        OriginalTag o = (OriginalTag) getIfRecorded(obj);
        if (null == o || !(obj instanceof Tag))
            return;

        Tag tag = (Tag) obj;
        JsonObject json = new JsonObject();

        if (!Utils.equal(tag.getName(), o.name))
            json.addProperty("name", o.name);

        if (!Utils.equal(tag.getReadme(), o.readme))
            json.addProperty("readme", o.readme);

        if (!Utils.equal(tag.getTs(), o.ts))
            json.addProperty("ts", o.ts);

        markForNotification();
        tag.diffJson = json.toString();
    }


    private static class OriginalTag
        extends OriginalAPersistent
    {
        String name;
        String readme;
        Long ts;
    }
}
