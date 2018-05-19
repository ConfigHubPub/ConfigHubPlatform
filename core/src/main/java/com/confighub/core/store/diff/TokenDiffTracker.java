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
import com.confighub.core.security.SecurityProfile;
import com.confighub.core.security.Token;
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

/**
 *
 */
public class TokenDiffTracker
        extends ADiffTracker
{
    private static final Logger log = LogManager.getLogger(TokenDiffTracker.class);

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
        if (!isTracked() || !(obj instanceof Token))
            return;

        OriginalToken o = new OriginalToken();
        Token token = (Token) obj;

        o.name = token.getName();
        o.active = token.isActive();
        o.expires = token.getExpires();
        o.forceKeyPushEnabled = token.isForceKeyPushEnabled();

        Set<SecurityProfile> sps = token.getSecurityProfiles();
        if (null != sps)
        {
            o.securityProfiles = new HashSet<>();
            sps.forEach(sp -> o.securityProfiles.add(sp.getName()));
        }

        o.teamRules = token.getTeamRules();
        o.managingTeam = token.getManagingTeam();
        o.user = token.getUser();
        o.managedBy = token.getManagedBy();

        obj.revType = APersisted.RevisionType.Modify;
        edits.get().put(obj.getId(), o);
    }

    @PreUpdate
    public void preUpdate(APersisted obj)
    {
        OriginalToken o = (OriginalToken) getIfRecorded(obj);
        if (null == o || !(obj instanceof Token))
            return;

        Token token = (Token) obj;
        JsonObject json = new JsonObject();

        if (!Utils.equal(token.getName(), o.name))
            json.addProperty("name", o.name);

        if (token.isActive() != o.active)
            json.addProperty("active", o.active);

        if (!Utils.equal(token.getExpires(), o.expires))
            json.addProperty("expires", null == o.expires ? "" : o.expires.toString());

        if (token.isForceKeyPushEnabled() != o.forceKeyPushEnabled)
            json.addProperty("forceKeyPushEnabled", o.forceKeyPushEnabled);

        boolean hadSps = null != o.securityProfiles && o.securityProfiles.size() > 0;
        boolean hasSps = null != token.getSecurityProfiles() && token.getSecurityProfiles().size() > 0;

        if (!hadSps && !hasSps) ;
        else if (hadSps && !hasSps)
        {
            JsonArray sps = new JsonArray();
            o.securityProfiles.forEach(sp -> sps.add(sp));
            json.add("sps", sps);
        }
        else if (!hadSps && hasSps)
        {
            json.add("sps", new JsonArray());
        }
        else
        {
            Set<String> current = new HashSet<>();
            token.getSecurityProfiles().forEach(sp -> current.add(sp.getName()));
            current.removeAll(o.securityProfiles);

            if (current.size() > 0)
            {
                JsonArray sps = new JsonArray();
                o.securityProfiles.forEach(sp -> sps.add(sp));
                json.add("sps", sps);
            }
            else
            {
                Set<String> clone = new HashSet<>();
                o.securityProfiles.forEach(sp -> clone.add(new String(sp)));

                current.clear();
                token.getSecurityProfiles().forEach(sp -> clone.add(sp.getName()));

                clone.removeAll(current);
                if (clone.size() > 0)
                {
                    JsonArray sps = new JsonArray();
                    o.securityProfiles.forEach(sp -> sps.add(sp));
                    json.add("sps", sps);
                }
            }
        }

        if (!Utils.equal(token.getTeamRules(), o.teamRules))
            json.addProperty("rulesTeam", null == o.teamRules ? "" : o.teamRules.getName());

        if (!Utils.equal(token.getManagingTeam(), o.managingTeam))
            json.addProperty("managingTeam", null == o.managingTeam ? "" : o.managingTeam.getName());

        if (!Utils.equal(token.getUser(), o.user))
            json.addProperty("user", null == o.user ? "" : o.user.getUsername());

        if (!Utils.equal(token.getManagedBy(), o.managedBy))
            json.addProperty("managedBy", o.managedBy.name());

        markForNotification();
        token.diffJson = json.toString();
    }

    private static class OriginalToken
            extends OriginalAPersistent
    {
        String name;
        boolean active;
        Long expires;
        boolean forceKeyPushEnabled;
        Set<String> securityProfiles;
        Team teamRules;
        Team managingTeam;
        UserAccount user;
        Token.ManagedBy managedBy;
    }
}