/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.core.store.diff;

import com.confighub.core.repository.Depth;
import com.confighub.core.repository.Level;
import com.confighub.core.store.APersisted;
import com.confighub.core.utils.Utils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
import java.util.HashSet;
import java.util.Set;

public class LevelDiffTracker
    extends ADiffTracker
{
    private static final Logger log = LogManager.getLogger(LevelDiffTracker.class);

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

    @PostLoad
    public void loadOldData(APersisted obj)
    {
        if (!isTracked() || !(obj instanceof Level))
            return;

        OriginalLevel o = new OriginalLevel();
        Level level = (Level) obj;

        o.name = level.getName();
        o.levelType = level.getType();
        o.depth = level.getDepth();

        if (level.isGroup())
        {
            Set<Level> kids = level.getMembers();
            if (null != kids && kids.size() > 0)
            {
                o.assigned = new HashSet<>();
                kids.forEach(k -> o.assigned.add(k.getName()));
            }
        }
        else if (level.isMember())
        {
            Set<Level> parents = level.getGroups();
            if (null != parents && parents.size() > 0)
            {
                o.assigned = new HashSet<>();
                parents.forEach(p -> o.assigned.add(p.getName()));
            }
        }

        obj.revType = APersisted.RevisionType.Modify;
        edits.get().put(obj.getId(), o);
    }

    @PreUpdate
    public void preUpdate(APersisted obj)
    {
        OriginalLevel o = (OriginalLevel)getIfRecorded(obj);
        if (null == o || !(obj instanceof Level))
            return;

        Level level = (Level) obj;

        JsonObject json = new JsonObject();

        if (!Utils.equal(level.getName(), o.name))
            json.addProperty("name", o.name);

        if (level.getDepth() != o.depth)
            json.addProperty("o.depth", o.depth.getPlacement());

        if (level.getType() != o.levelType)
        {
            json.addProperty("type", o.levelType.name());
        }

        boolean hadAssignments = null != o.assigned && o.assigned.size() > 0;
        boolean hasAssignments = null != level.getMembers() && level.getMembers().size() > 0;

        // did not have assignments, and still does not
        // ---> do nothing
        if (!hadAssignments && !hasAssignments)
            ;

        // had assignments, but now it does not
        // ---> add old assignments to JSON
        else if (hadAssignments && !hasAssignments)
        {
            JsonArray assignments = new JsonArray();
            o.assigned.forEach(c -> assignments.add(c));
            json.add("assignments", assignments);
        }

        // did not have assignments, but now it does
        // ---> add empty list for assignments
        else if (!hadAssignments && hasAssignments)
        {
            json.add("assignments", new JsonArray());
        }

        else
        {
            Set<String> current = new HashSet<>();
            level.getMembers().forEach(k -> current.add(k.getName()));
            current.removeAll(o.assigned);

            // different assignments
            // ---> add old assignments to JSON
            if (current.size() > 0)
            {
                JsonArray assignments = new JsonArray();
                o.assigned.forEach(c -> assignments.add(c));
                json.add("assignments", assignments);
            }
            else
            {
                Set<String> clone = new HashSet<>();
                o.assigned.forEach(ln -> clone.add(new String(ln)));

                current.clear();
                level.getMembers().forEach(k -> current.add(k.getName()));

                clone.removeAll(current);
                if (clone.size() > 0) {
                    JsonArray assignments = new JsonArray();
                    o.assigned.forEach(c -> assignments.add(c));
                    json.add("assignments", assignments);
                }

                // assignments the same
                // ---> do nothing
            }
        }

        level.diffJson = json.toString();
    }

    private static class OriginalLevel
            extends OriginalAPersistent
    {
        String name;
        Level.LevelType levelType;
        Depth depth;
        Set<String> assigned;

        @Override
        public String toString()
        {
            return String.format("%s | %s | %s ",
                                 name,
                                 levelType.name(),
//                                 depth.name(),
                                 null == assigned ? "" : Utils.join(assigned, ", "));
        }
    }
}