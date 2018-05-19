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

package com.confighub.core.rules;

import com.confighub.core.organization.Team;
import com.confighub.core.repository.AContextAwarePersistent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.easyrules.core.AbstractRulesEngine;
import org.easyrules.core.AnnotatedRulesEngine;

import java.util.List;

/**
 *
 */
public class AccessRuleWrapper
{
    private static final Logger log = LogManager.getLogger(AccessRuleWrapper.class);

    private final  List<AccessRule> accessRules;
    private final Team team;
    private final AnnotatedRulesEngine rulesEngine;

    public AccessRuleWrapper(final Team team, final AccessRule.RuleTarget filter)
    {
        this.accessRules = team.getAccessRules(filter);

        this.rulesEngine = new AnnotatedRulesEngine(team.isStopOnFirstMatch());
        this.team = team;

        java.util.logging.Logger.getLogger(AnnotatedRulesEngine.class.getName()).setLevel(java.util.logging.Level.OFF);
        java.util.logging.Logger.getLogger(AbstractRulesEngine.class.getName()).setLevel(java.util.logging.Level.OFF);

        accessRules.forEach(rulesEngine::registerRule);
    }

    public boolean executeRuleFor(final AContextAwarePersistent contextAwarePersistent)
    {
        contextAwarePersistent.isEditable = team.isUnmatchedEditable();
        contextAwarePersistent.resetTransient();
        RuleResponse ruleResponse = new RuleResponse(true);
        accessRules.forEach(rule -> rule.setContextAwarePersistent(contextAwarePersistent, ruleResponse));
        rulesEngine.fireRules();

        return ruleResponse.isEditable;
    }

    /**
     * This method is called from KeyUtils.isKeyEditable when a user is trying to create a new key.
     * Since this key does not exist, only check if there is an explicit rule preventing this key
     * from being created.  Ignore the global matching rule.
     *
     * @param key
     * @return
     */
    public boolean executeRuleFor(final String key)
    {
        RuleResponse ruleResponse = new RuleResponse(true);
        accessRules.forEach(rule -> rule.setKey(key, ruleResponse));
        rulesEngine.fireRules();

        return ruleResponse.isEditable;
    }

    protected static class RuleResponse
    {
        public boolean isEditable;
        RuleResponse(boolean editable) {
            this.isEditable = editable;
        }
    }

    public Team getTeam()
    {
        return team;
    }

    public List<AccessRule> getAccessRules()
    {
        return accessRules;
    }
}
