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

import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.organization.Team;
import com.confighub.core.repository.AContextAwarePersistent;
import com.confighub.core.repository.Depth;
import com.confighub.core.repository.CtxLevel;
import com.confighub.core.repository.Property;
import com.confighub.core.resolver.AResolver;
import com.confighub.core.store.APersisted;
import com.confighub.core.store.diff.AccessRuleDiffTracker;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.easyrules.annotation.Action;
import org.easyrules.annotation.Condition;
import org.easyrules.annotation.Rule;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import javax.persistence.*;
import java.util.*;


@Rule
@Entity
@Table( name = "accessrule" )
@Cacheable
@Cache( usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE )
@NamedQueries( { @NamedQuery( name = "Rule.byId",
                              query = "SELECT r FROM AccessRule r WHERE team=:team AND id=:id" ), } )
@EntityListeners( { AccessRuleDiffTracker.class } )
@Audited
@AuditTable( "accessrule_audit" )
public class AccessRule
      extends APersisted
      implements Comparable
{
    private static final Logger log = LogManager.getLogger( AccessRule.class );


    @Override
    public int compareTo( Object o )
    {
        AccessRule other = (AccessRule) o;
        if ( this.priority == other.priority )
        {
            return 0;
        }
        if ( this.priority > other.priority )
        {
            return 1;
        }
        return -1;
    }


    public enum KeyMatchType
    {
        Is, StartsWith, EndsWith, Contains
    }

    public enum ContextMatchType
    {
        Resolves, DoesNotResolve, ContainsAny, ContainsAll, DoesNotContain
    }

    public enum RuleTarget
    {
        Key, Value;
    }

    @Id
    @GeneratedValue
    private Long id;

    @Column( nullable = false )
    private int priority;

    @Enumerated( EnumType.STRING )
    @Column( nullable = false,
             name = "ruleTarget" )
    private RuleTarget ruleTarget;

    @Enumerated( EnumType.STRING )
    @Column( name = "keyMatchType" )
    private KeyMatchType keyMatchType;

    @Enumerated( EnumType.STRING )
    @Column( name = "contextMatchType" )
    private ContextMatchType contextMatchType;

    @Column( name = "matchValue" )
    private String matchValue;

    @ManyToMany( fetch = FetchType.LAZY,
                 cascade = { CascadeType.REFRESH } )
    @NotAudited
    private Set<CtxLevel> context;

    // ENVERS optimization
    @Lob
    @Type( type = "org.hibernate.type.TextType" )
    @Column( nullable = false,
             columnDefinition = "TEXT" )
    private String contextJson;

    @ManyToOne( fetch = FetchType.LAZY,
                cascade = { CascadeType.REFRESH } )
    @JoinColumn( nullable = false,
                 name = "team" )
    private Team team;

    @Column( nullable = false )
    private boolean canEdit;

    private transient AContextAwarePersistent contextAwarePersistent;


    public void setContextAwarePersistent( AContextAwarePersistent contextAwarePersistent,
                                           AccessRuleWrapper.RuleResponse ruleResponse )
    {
        this.contextAwarePersistent = contextAwarePersistent;
        this.ruleResponse = ruleResponse;
        if ( contextAwarePersistent instanceof Property )
        {
            Property property = (Property) contextAwarePersistent;
            this.key = property.getKey();
        }
    }


    private transient String key;

    private transient AccessRuleWrapper.RuleResponse ruleResponse;


    public void setKey( String key,
                        AccessRuleWrapper.RuleResponse ruleResponse )
    {
        this.key = key;
        this.ruleResponse = ruleResponse;
    }


    protected AccessRule()
    {
    }


    public AccessRule( final Team team,
                       final RuleTarget ruleTarget,
                       final ContextMatchType contextMatchType,
                       final Set<CtxLevel> context,
                       final boolean canEdit,
                       final int priority )
          throws ConfigException
    {
        if ( Utils.anyNull( ruleTarget, contextMatchType ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        this.team = team;
        this.ruleTarget = ruleTarget;
        this.contextMatchType = contextMatchType;
        this.keyMatchType = null;
        this.matchValue = null;
        this.context = context;
        this.contextJson = contextToJson().toString();
        this.canEdit = canEdit;
        this.priority = priority;
    }


    public AccessRule( final Team team,
                       final RuleTarget ruleTarget,
                       final KeyMatchType keyMatchType,
                       final String matchValue,
                       final boolean canEdit,
                       final int priority )
          throws ConfigException
    {
        if ( Utils.anyNull( ruleTarget, keyMatchType, matchValue ) || Utils.isBlank( matchValue ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        this.team = team;
        this.ruleTarget = ruleTarget;
        this.keyMatchType = keyMatchType;
        this.contextMatchType = null;
        this.matchValue = matchValue;
        this.context = null;
        this.contextJson = contextToJson().toString();
        this.canEdit = canEdit;
        this.priority = priority;
    }


    private transient TreeMap<Depth, Collection<CtxLevel>> depthMap = null;


    public TreeMap<Depth, Collection<CtxLevel>> getDepthMap( boolean includeClusters )
    {
        if ( null == this.context )
        {
            return null;
        }

        if ( null != depthMap )
        {
            return depthMap;
        }

        depthMap = new TreeMap<>();
        for ( CtxLevel ctxLevel : this.context )
        {
            Depth depth = ctxLevel.getDepth();
            if ( !depthMap.containsKey( depth ) )
            {
                depthMap.put( depth, new HashSet<>() );
            }

            depthMap.get( depth ).add( ctxLevel );

            // Add all node's clusters
            if ( includeClusters && ctxLevel.isMember() && null != ctxLevel.getGroups() )
            {
                depthMap.get( depth ).addAll( ctxLevel.getGroups() );
            }
        }

        return depthMap;
    }


    @Condition
    public boolean when()
    {
        if ( RuleTarget.Key.equals( ruleTarget ) )
        {
            switch ( this.keyMatchType )
            {
                case Is:
                    return this.key.equalsIgnoreCase( matchValue );

                case StartsWith:
                    return this.key.toLowerCase().startsWith( matchValue.toLowerCase() );

                case EndsWith:
                    return this.key.toLowerCase().endsWith( matchValue.toLowerCase() );

                case Contains:
                    return this.key.toLowerCase().contains( matchValue.toLowerCase() );
            }
        }
        else if ( RuleTarget.Value.equals( ruleTarget ) )
        {
            switch ( contextMatchType )
            {
                case Resolves:
                    return AResolver.isContextualMatch( contextAwarePersistent.getDepthMap(), getDepthMap( true ) );

                case DoesNotResolve:
                    return !AResolver.isContextualMatch( contextAwarePersistent.getDepthMap(), getDepthMap( true ) );

                case ContainsAny:
                    return AResolver.containsAny( contextAwarePersistent.getDepthMap(), getDepthMap( true ) );

                case DoesNotContain:
                    return !AResolver.containsAny( contextAwarePersistent.getDepthMap(), getDepthMap( true ) );

                case ContainsAll:
                    return AResolver.containsAll( contextAwarePersistent.getDepthMap(), getDepthMap( true ) );
            }
        }

        return false;
    }


    @Action
    public void then()
    {
        if ( null != ruleResponse )
        {
            ruleResponse.isEditable = canEdit;
        }

        if ( null != contextAwarePersistent )
        {
            contextAwarePersistent.isEditable = canEdit;
        }
    }


    @Override
    public String toString()
    {
        return String.format( "%d | %s | %s | %s",
                              null == contextAwarePersistent || null == contextAwarePersistent.getId()
                              ? -1L : contextAwarePersistent.getId(),
                              null == ruleTarget ? "n/a" : ruleTarget.name(),
                              null == keyMatchType ? "n/a" : keyMatchType.name(),
                              matchValue );
    }


    public JsonObject contextToJson()
    {
        JsonObject contextJ = new JsonObject();
        if ( null != this.getContext() )
        {
            TreeMap<Depth, Collection<CtxLevel>> contextMap = this.getDepthMap( false );

            if ( null != contextMap )
            {
                for ( Depth depth : contextMap.keySet() )
                {
                    JsonArray depthLevelsJ = new JsonArray();
                    for ( CtxLevel ctxLevel : contextMap.get( depth ) )
                    {
                        depthLevelsJ.add( ctxLevel.getName() );
                    }

                    contextJ.add( String.valueOf( depth.getPlacement() ), depthLevelsJ );
                }
            }
        }

        return contextJ;
    }


    // --------------------------------------------------------------------------------------------
    // Setters and getters
    // --------------------------------------------------------------------------------------------
    @Override
    public Long getId()
    {
        return id;
    }


    public int getPriority()
    {
        return priority;
    }


    public void setPriority( int priority )
    {
        this.priority = priority;
    }


    public RuleTarget getRuleTarget()
    {
        return ruleTarget;
    }


    public KeyMatchType getKeyMatchType()
    {
        return keyMatchType;
    }


    public String getMatchValue()
    {
        return matchValue;
    }


    public ContextMatchType getContextMatchType()
    {
        return contextMatchType;
    }


    public void setContextMatchType( ContextMatchType contextMatchType )
    {
        this.contextMatchType = contextMatchType;
    }


    public boolean isCanEdit()
    {
        return canEdit;
    }


    public Set<CtxLevel> getContext()
    {
        return context;
    }


    public void setContext( Set<CtxLevel> context )
    {
        if ( !Utils.same( this.context, context ) )
        {
            this.context = context;
            this.contextJson = contextToJson().toString();
        }
    }


    public Team getTeam()
    {
        return team;
    }


    public void setRuleTarget( RuleTarget ruleTarget )
    {
        this.ruleTarget = ruleTarget;
    }


    public void setKeyMatchType( KeyMatchType keyMatchType )
    {
        this.keyMatchType = keyMatchType;
    }


    public void setMatchValue( String matchValue )
    {
        this.matchValue = matchValue;
    }


    public void setCanEdit( boolean canEdit )
    {
        this.canEdit = canEdit;
    }


    public JsonObject getContextJsonObj()
    {
        if ( null == this.contextJson )
        {
            return new JsonObject();
        }

        return new Gson().fromJson( this.contextJson, JsonObject.class );
    }


    @Override
    public ClassName getClassName()
    {
        return ClassName.AccessRule;
    }
}
