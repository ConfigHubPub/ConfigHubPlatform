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
