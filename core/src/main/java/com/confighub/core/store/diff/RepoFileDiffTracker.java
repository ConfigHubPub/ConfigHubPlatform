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
