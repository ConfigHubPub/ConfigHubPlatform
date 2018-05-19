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

import com.confighub.core.rules.AccessRule;
import com.confighub.core.store.APersisted;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.*;

/**
 *
 */
public class AccessRuleDiffTracker
        extends ADiffTracker
{
    private static final Logger log = LogManager.getLogger(AccessRuleDiffTracker.class);

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
        if (!isTracked() || !(obj instanceof AccessRule))
            return;

        AccessRule rule = (AccessRule) obj;
        OriginalAccessRule o = new OriginalAccessRule();

        o.priority = rule.getPriority();
        o.ruleTarget = rule.getRuleTarget();
        o.canEdit = rule.isCanEdit();

        if (AccessRule.RuleTarget.Key.equals(o.ruleTarget))
        {
            o.keyMatchType = rule.getKeyMatchType();
            o.matchValue = rule.getMatchValue();
        }
        else
        {
            o.contextMatchType = rule.getContextMatchType();
            o.contextJson = null == rule.getContextJsonObj() ? "" : rule.getContextJsonObj().toString();
        }

        obj.revType = APersisted.RevisionType.Modify;
        edits.get().put(obj.getId(), o);
    }

    @PreUpdate
    public void preUpdate(APersisted obj)
    {
        OriginalAccessRule o = (OriginalAccessRule)getIfRecorded(obj);
        if (null == o || !(obj instanceof AccessRule))
            return;

        AccessRule rule = (AccessRule) obj;

        JsonObject json = new JsonObject();

        if (o.priority != rule.getPriority())
            json.addProperty("priority", o.priority);

        if (!Utils.same(o.ruleTarget, rule.getRuleTarget()))
            json.addProperty("type", Utils.jsonString(o.ruleTarget));

        if (AccessRule.RuleTarget.Key.equals(o.ruleTarget))
        {
            if (!Utils.same(o.keyMatchType, rule.getKeyMatchType()))
                json.addProperty("match", Utils.jsonString(o.keyMatchType));

            if (!Utils.same(o.matchValue, rule.getMatchValue()))
                json.addProperty("key", Utils.jsonString(o.matchValue));
        }
        else
        {
            if (!Utils.same(o.contextMatchType, rule.getContextMatchType()))
                json.addProperty("match", Utils.jsonString(o.contextMatchType));

            if (!Utils.same(o.contextJson, rule.getContextJsonObj().toString()))
                json.add("context", new Gson().fromJson(o.contextJson, JsonObject.class));
        }

        if (!Utils.same(o.matchValue, rule.getMatchValue()))
            json.addProperty("key", Utils.jsonString(o.matchValue));

        if (o.canEdit != rule.isCanEdit())
            json.addProperty("access", o.canEdit ? "rw" : "ro");

        markForNotification();
        rule.diffJson = json.toString();
    }

    private String toText(final AccessRule rule)
    {
        StringBuilder sb = new StringBuilder();

        sb.append(rule.isCanEdit() ? "Read & Write " : "Read-Only ")
          .append("when ");

        switch (rule.getRuleTarget())
        {
            case Value:
                sb.append("context ");
                switch (rule.getContextMatchType())
                {
                    case Resolves: sb.append("is "); break;
                    case DoesNotResolve: sb.append("does not resolve "); break;
                    case ContainsAny: sb.append("contains any"); break;
                    case ContainsAll: sb.append("contains all"); break;
                    case DoesNotContain: sb.append("does not contain "); break;
                }



                break;

            case Key:
                sb.append("key ");

                switch (rule.getKeyMatchType())
                {
                    case Is: sb.append("is "); break;
                    case StartsWith: sb.append("starts with "); break;
                    case EndsWith: sb.append("ends with "); break;
                    case Contains: sb.append("contains "); break;
                }

                sb.append("'").append(rule.getMatchValue()).append("'");
                break;
        }


        return sb.toString();
    }

    private static class OriginalAccessRule
            extends OriginalAPersistent
    {
        int priority;
        AccessRule.RuleTarget ruleTarget;
        AccessRule.KeyMatchType keyMatchType;
        AccessRule.ContextMatchType contextMatchType;
        String matchValue;
        String contextJson;
        boolean canEdit;
    }

}
