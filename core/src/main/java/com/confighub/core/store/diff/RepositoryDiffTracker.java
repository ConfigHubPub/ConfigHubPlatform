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

import com.confighub.core.repository.Depth;
import com.confighub.core.repository.Repository;
import com.confighub.core.store.APersisted;
import com.confighub.core.utils.Utils;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class RepositoryDiffTracker
        extends ADiffTracker
{
    private static final Logger log = LogManager.getLogger(RepositoryDiffTracker.class);

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
        if (!isTracked() || !(obj instanceof Repository))
            return;

        OriginalRepository o = new OriginalRepository();
        Repository repository = (Repository) obj;

        o.name = repository.getName();
        o.description = repository.getDescription();
        o.depth = repository.getDepth();
        o.accountName = repository.getAccount().getName();
        o.accessControlEnabled = repository.isAccessControlEnabled();
        o.valueTypeEnabled = repository.isValueTypeEnabled();
        o.securityProfilesEnabled = repository.isSecurityProfilesEnabled();
        o.contextClustersEnabled = repository.isContextClustersEnabled();
        o.isPrivate = repository.isPrivate();

        o.labels = new HashMap<>();
        for (Depth depth : repository.getDepth().getDepths())
            o.setLabel(depth, repository.getLabel(depth));

        obj.revType = APersisted.RevisionType.Modify;
        edits.get().put(obj.getId(), o);
    }

    @PreUpdate
    public void preUpdate(APersisted obj)
    {
        if (!isTracked() || !(obj instanceof Repository))
            return;

        OriginalRepository o = (OriginalRepository)getIfRecorded(obj);
        if (null == o)
            return;

        JsonObject json = new JsonObject();
        Repository repository = (Repository)obj;

        if (null == repository.getAccount())
            json.addProperty("deleted", true);
        else if (!repository.getAccount().getName().equalsIgnoreCase(o.accountName))
            json.addProperty("owner", o.accountName);

        if (!Utils.equal(repository.getName(), o.name))
            json.addProperty("name", o.name);

        if (!Utils.equal(repository.getDescription(), o.description))
            json.addProperty("description", o.description);

        if (repository.isAccessControlEnabled() != o.accessControlEnabled)
            json.addProperty("accessControlEnabled", o.accessControlEnabled);

        if (repository.isValueTypeEnabled() != o.valueTypeEnabled)
            json.addProperty("valueTypeEnabled", o.valueTypeEnabled);

        if (repository.isSecurityProfilesEnabled() != o.securityProfilesEnabled)
            json.addProperty("securityProfilesEnabled", o.securityProfilesEnabled);

        if (repository.isContextClustersEnabled() != o.contextClustersEnabled)
            json.addProperty("contextClustersEnabled", o.contextClustersEnabled);

        if (repository.isPrivate() != o.isPrivate)
            json.addProperty("private", o.isPrivate);

        if (repository.getDepth() != o.depth)
        {
            JsonObject labels = new JsonObject();
            for (Depth depth : repository.getDepth().getDepths())
                labels.addProperty(String.valueOf(depth.getPlacement()), o.labels.get(depth));

            json.add("labels", labels);
            json.addProperty("depth", repository.getDepth().name());

            setContextResize();
        }

        for (Depth depth : repository.getDepth().getDepths())
        {
            if (!Utils.equal(o.labels.get(depth), repository.getLabel(depth)))
            {
                JsonObject labels = new JsonObject();
                for (Depth d : repository.getDepth().getDepths())
                    labels.addProperty(String.valueOf(d.getPlacement()), o.labels.get(d));

                json.add("labels", labels);
                break;
            }
        }

        markForNotification();
        repository.diffJson = json.toString();
    }

    private static class OriginalRepository
            extends OriginalAPersistent
    {
        String name;
        String description;
        Depth depth;
        String accountName;
        boolean accessControlEnabled;
        boolean valueTypeEnabled;
        boolean securityProfilesEnabled;
        boolean contextClustersEnabled;
        boolean isPrivate;
        Map<Depth, String> labels;

        void setLabel(Depth depth, String label)
        {
            if (null == labels)
                labels = new HashMap<>();

            labels.put(depth, label);
        }

        @Override
        public String toString()
        {
            return String.format("%s: vdt: %s, | labels: %s",
                                 this.name,
                                 this.valueTypeEnabled,
                                 Utils.join(labels.values(), " > "));
        }
    }
}
