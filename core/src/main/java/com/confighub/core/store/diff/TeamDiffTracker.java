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

import com.confighub.core.organization.Team;
import com.confighub.core.store.APersisted;
import com.confighub.core.user.UserAccount;
import com.confighub.core.utils.Utils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

public class TeamDiffTracker
        extends ADiffTracker
{
    private static final Logger log = LogManager.getLogger(TeamDiffTracker.class);

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
    public void loadData(APersisted obj)
    {
        if (!isTracked() || !(obj instanceof Team))
            return;

        OriginalTeam o = new OriginalTeam();
        Team team = (Team) obj;

        o.name = team.getName();
        o.stopOnFirstMatch = team.isStopOnFirstMatch();
        o.unmatchedEditable = team.isUnmatchedEditable();

        Set<UserAccount> users = team.getMembers();
        if (null != users)
        {
            o.members = new HashSet<>();
            o.members.addAll(users);
        }

        obj.revType = APersisted.RevisionType.Modify;
        edits.get().put(obj.getId(), o);
    }

    @PreUpdate
    public void preUpdate(APersisted obj)
    {
        OriginalTeam o = (OriginalTeam) getIfRecorded(obj);
        if (null == o || !(obj instanceof Team))
            return;

        Team team = (Team) obj;
        JsonObject json = new JsonObject();

        if (!Utils.equal(team.getName(), o.name))
            json.addProperty("name", o.name);

        if (team.isStopOnFirstMatch() != o.stopOnFirstMatch)
            json.addProperty("stopOnFirstMatch", o.stopOnFirstMatch);

        if (team.isUnmatchedEditable() != o.unmatchedEditable)
            json.addProperty("unmatchedEditable", o.unmatchedEditable);

        Set<UserAccount> currentMembers = team.getMembers();
        if (null == o.members || o.members.size() == 0)
        {
            if (null != currentMembers && currentMembers.size() > 0)
            {
                JsonArray added = new JsonArray();
                currentMembers.forEach(m -> added.add(userToJson(m)));
                json.add("newMembers", added);
            }
        }
        else
        {
            if (null == currentMembers || currentMembers.size() == 0)
            {
                JsonArray removed = new JsonArray();
                o.members.forEach(m -> removed.add(userToJson(m)));
                json.add("removedMembers", removed);
            }
            else
            {
                Set<UserAccount> copyOfCurrent = new HashSet<>();
                currentMembers.forEach(m -> copyOfCurrent.add(m));

                copyOfCurrent.removeAll(o.members);
                if (copyOfCurrent.size() > 0)
                {
                    JsonArray added = new JsonArray();
                    copyOfCurrent.forEach(m -> added.add(userToJson(m)));
                    json.add("newMembers", added);
                }

                o.members.removeAll(currentMembers);
                if (o.members.size() > 0)
                {
                    JsonArray removed = new JsonArray();
                    o.members.forEach(m -> removed.add(userToJson(m)));
                    json.add("removedMembers", removed);
                }
            }
        }

        markForNotification();
        team.diffJson = json.toString();
    }

    private JsonObject userToJson(UserAccount u)
    {
        JsonObject json = new JsonObject();
        json.addProperty("id", u.getId());
        json.addProperty("un", u.getUsername());
        json.addProperty("nm", u.getName());

        return json;
    }

    private static class OriginalTeam
            extends OriginalAPersistent
    {
        String name;
        Set<UserAccount> members;
        boolean stopOnFirstMatch;
        boolean unmatchedEditable;
    }

}
