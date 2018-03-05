/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.core.store.diff;

import com.confighub.core.repository.RepoFile;
import com.confighub.core.store.APersisted;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.persistence.*;

/**
 *
 */
public class RepoFileDiffTracker
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
        if (!isTracked() || !(obj instanceof RepoFile))
            return;

        OriginalRepoFile o = new OriginalRepoFile();
        RepoFile file = (RepoFile)obj;

        o.absPath = file.getAbsPath();
        o.contextJson = file.getContextJson();
        o.content = file.getContent();
        o.spName = null == file.getSecurityProfile() ? null : file.getSecurityProfile().getName();
        o.active = file.isActive();
        o.encrypted = file.isEncrypted();

        obj.revType = APersisted.RevisionType.Modify;
        edits.get().put(obj.getId(), o);
    }

    @PreUpdate
    public void preUpdate(APersisted obj)
    {
        OriginalRepoFile o = (OriginalRepoFile)getIfRecorded(obj);
        if (null == o || !(obj instanceof RepoFile))
            return;

        RepoFile file = (RepoFile)obj;
        JsonObject json = new JsonObject();

        if (!Utils.equal(o.content, file.getContent()))
        {
            json.addProperty("content", o.content);
            json.addProperty("encrypted", o.encrypted);
        }

        if (!Utils.equal(o.absPath, file.getAbsPath()))
            json.addProperty("absPath", o.absPath);

        if (!Utils.equal(o.contextJson, file.getContextJson()))
            json.add("context", new Gson().fromJson(o.contextJson, JsonArray.class));

        if (!Utils.isBlank(o.spName))
            json.addProperty("spName", o.spName);

        if (o.active != file.isActive())
            json.addProperty("active", o.active);

        file.diffJson = json.toString();
    }

    private static class OriginalRepoFile
            extends OriginalAPersistent
    {
        String absPath;
        String contextJson;
        String content;
        String spName;
        boolean active;
        boolean encrypted;
    }
}
