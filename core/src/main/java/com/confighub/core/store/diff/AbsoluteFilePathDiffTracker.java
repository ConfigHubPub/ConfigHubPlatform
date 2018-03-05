/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.core.store.diff;

import com.confighub.core.repository.AbsoluteFilePath;
import com.confighub.core.store.APersisted;
import com.confighub.core.utils.Utils;
import com.google.gson.JsonObject;

import javax.persistence.*;

/**
 *
 */
public class AbsoluteFilePathDiffTracker
        extends ADiffTracker
{
    @PreRemove
    public void preRemove(APersisted obj)
    {
        obj.revType = APersisted.RevisionType.Delete;
    }

    @PrePersist
    public void prePersist(APersisted obj)
    {
        obj.revType = APersisted.RevisionType.Add;
    }

    @PostPersist
    public void postPersist(APersisted obj)
    {

    }

    @PostLoad
    public void loadOldData(APersisted obj)
    {
        if (!isTracked() || !(obj instanceof AbsoluteFilePath))
            return;

        AbsoluteFilePath absoluteFilePath = (AbsoluteFilePath)obj;

        OriginalAbsFilePath o = new OriginalAbsFilePath();
        o.absFilePath = absoluteFilePath.getAbsPath();

        obj.revType = APersisted.RevisionType.Modify;
        edits.get().put(obj.getId(), o);
    }

    @PreUpdate
    public void preUpdate(APersisted obj)
    {
        OriginalAbsFilePath o = (OriginalAbsFilePath)getIfRecorded(obj);
        if (null == o || !(obj instanceof AbsoluteFilePath))
            return;

        JsonObject json = new JsonObject();
        AbsoluteFilePath absoluteFilePath = (AbsoluteFilePath)obj;

        if (!Utils.equal(o.absFilePath, absoluteFilePath.getAbsPath()))
            json.addProperty("absPath", o.absFilePath);

        absoluteFilePath.diffJson = json.toString();
    }

    private static class OriginalAbsFilePath
            extends OriginalAPersistent
    {
        String absFilePath;
    }
}
