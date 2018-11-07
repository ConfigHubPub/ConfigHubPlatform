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

package com.confighub.core.organization;

import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.repository.Repository;
import com.confighub.core.rules.AccessRule;
import com.confighub.core.security.Token;
import com.confighub.core.store.APersisted;
import com.confighub.core.store.diff.TeamDiffTracker;
import com.confighub.core.user.UserAccount;
import com.confighub.core.utils.Utils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;


@Entity
@Cacheable
@Cache( usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE )
@Table( name = "team",
        uniqueConstraints = @UniqueConstraint( columnNames = { "name",
                                                               "repositoryId" } ) )
@NamedQueries(
      {
            @NamedQuery( name = "Team.get",
                         query = "SELECT t from Team t WHERE repository=:repository AND name=:name" ),

            @NamedQuery( name = "Team.forMember",
                         query = "SELECT t from Team t WHERE repository=:repository AND :member MEMBER of t.members" )
      } )
@Audited
@AuditTable( "team_audit" )
@EntityListeners( { TeamDiffTracker.class } )
public class Team
      extends APersisted
{
    @Id
    @GeneratedValue
    private Long id;

    @Column( nullable = false )
    private String name;

    @Column( name = "description" )
    private String description;

    @ManyToOne( fetch = FetchType.LAZY,
                cascade = { CascadeType.REFRESH,
                            CascadeType.PERSIST } )
    @JoinColumn( nullable = false,
                 name = "repositoryId" )
    private Repository repository;

    @ManyToMany( fetch = FetchType.LAZY,
                 cascade = { CascadeType.REFRESH,
                             CascadeType.PERSIST } )
    @Audited( targetAuditMode = RelationTargetAuditMode.NOT_AUDITED )
    private Set<UserAccount> members;

    @ManyToMany( fetch = FetchType.LAZY,
                 cascade = { CascadeType.REFRESH,
                             CascadeType.PERSIST } )
    @Audited( targetAuditMode = RelationTargetAuditMode.NOT_AUDITED )
    private Set<Token> tokens;

    @Column( nullable = false )
    @NotAudited
    private Date createDate;

    @OneToMany( fetch = FetchType.LAZY,
                cascade = { CascadeType.REFRESH,
                            CascadeType.REMOVE,
                            CascadeType.PERSIST },
                mappedBy = "team" )
    @NotAudited
    private Set<AccessRule> accessRules;

    @Column( name = "stopOnFirstMatch" )
    private boolean stopOnFirstMatch = true;

    @Column( name = "unmatchedEditable" )
    private boolean unmatchedEditable = false;


    protected Team()
    {
    }


    public Team( Repository repository,
                 String name )
          throws ConfigException
    {
        if ( null == repository )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        this.name = name;
        this.repository = repository;
        this.repository.addTeam( this );
    }

    // Toggle this field forcing preUpdate.  Since Set<User> and Set<AccessRule> in a Team
    // are non-audited fields, this boolean is a trigger for this callback to trigger
    // the call to @PreUpdate.
    //    @Column(name="auditForceToggle")
    //    public boolean auditForceToggle;


    // --------------------------------------------------------------------------------------------
    // Rules
    // --------------------------------------------------------------------------------------------
    public void addRule( AccessRule accessRule )
    {
        if ( null == accessRule )
        {
            return;
        }
        if ( null == this.accessRules )
        {
            this.accessRules = new HashSet<>();
        }

        this.accessRules.add( accessRule );
    }


    public int getRuleCount()
    {
        if ( null == this.accessRules )
        {
            return 0;
        }
        return this.accessRules.size();
    }


    public AccessRule deleteRule( Long ruleId )
    {
        if ( null == ruleId || null == this.accessRules )
        {
            return null;
        }

        Iterator<AccessRule> itt = this.accessRules.iterator();
        while ( itt.hasNext() )
        {
            AccessRule rule = itt.next();
            if ( rule.getId().equals( ruleId ) )
            {
                itt.remove();
                return rule;
            }
        }

        return null;
    }


    // --------------------------------------------------------------------------------------------
    // Members
    // --------------------------------------------------------------------------------------------
    public void addMember( UserAccount user )
    {
        if ( null == user )
        {
            return;
        }

        if ( null == this.members )
        {
            this.members = new HashSet<>();
        }

        this.members.add( user );
    }


    public boolean removeMember( UserAccount user )
    {
        if ( null == user || null == this.members )
        {
            return false;
        }

        if ( !this.repository.isAdminOrOwner( user ) )
        {
            user.getAccount().removeRepository( this.repository );
        }

        return this.members.remove( user );
    }


    public boolean isMember( UserAccount user )
    {
        if ( null == user )
        {
            return false;
        }

        return null != this.members && this.members.contains( user );
    }


    public int getMemberCount()
    {
        if ( null == this.members )
        {
            return 0;
        }

        return this.members.size();
    }

    // --------------------------------------------------------------------------------------------
    // Validation
    // --------------------------------------------------------------------------------------------


    @PreUpdate
    @PrePersist
    public void validateName()
          throws ConfigException
    {
        this.createDate = new Date();

        if ( !Utils.isNameValid( this.name ) )
        {
            throw new ConfigException( Error.Code.ILLEGAL_CHARACTERS );
        }
    }


    @PreRemove
    public void preRemove()
    {
        if ( null != this.members )
        {
            Iterator<UserAccount> itt = this.members.iterator();
            while ( itt.hasNext() )
            {
                UserAccount member = itt.next();
                if ( !this.repository.isAdminOrOwner( member ) )
                {
                    member.getAccount().removeRepository( this.repository );
                }

                itt.remove();
            }
        }

        this.repository.removeTeam( this );
    }

    // --------------------------------------------------------------------------------------------
    // POJO Ops
    // --------------------------------------------------------------------------------------------


    @Override
    public boolean equals( Object o )
    {
        if ( null == o )
        {
            return false;
        }

        if ( !( o instanceof Team ) )
        {
            return false;
        }

        Team t = (Team) o;
        return this.repository.equals( t.getRepository() ) && this.name.equals( t.getName() );
    }


    @Override
    public String toString()
    {
        return String.format( "Team: %s | ruleCount: %d", this.name, getRuleCount() );
    }

    // --------------------------------------------------------------------------------------------
    // Setters and getters
    // --------------------------------------------------------------------------------------------


    @Override
    public Long getId()
    {
        return this.id;
    }


    public String getName()
    {
        return name;
    }


    public void setName( String name )
    {
        this.name = name;
    }


    public Date getCreateDate()
    {
        return createDate;
    }


    public Repository getRepository()
    {
        return repository;
    }


    public Set<UserAccount> getMembers()
    {
        return this.members;
    }


    public List<AccessRule> getAccessRules()
    {
        return getAccessRules( null );
    }


    public List<AccessRule> getAccessRules( final AccessRule.RuleTarget filter )
    {
        if ( null == this.accessRules )
        {
            return null;
        }

        ArrayList<AccessRule> sorted;

        if ( null != filter && AccessRule.RuleTarget.Key.equals( filter ) )
        {
            sorted = new ArrayList<>();
            sorted.addAll( accessRules.stream()
                                      .filter( rule -> rule.getRuleTarget().equals( AccessRule.RuleTarget.Key ) )
                                      .collect( Collectors.toList() ) );
        }
        else if ( null != filter && AccessRule.RuleTarget.Value.equals( filter ) )
        {
            sorted = new ArrayList<>();
            sorted.addAll( accessRules.stream()
                                      .filter( rule -> rule.getRuleTarget().equals( AccessRule.RuleTarget.Value ) )
                                      .collect( Collectors.toList() ) );
        }
        else
        {
            sorted = new ArrayList<>( this.accessRules );
        }

        Collections.sort( sorted );
        return sorted;
    }


    public Set<Token> getTokens()
    {
        return tokens;
    }


    public String getDescription()
    {
        return description;
    }


    public void setDescription( String description )
    {
        this.description = description;
    }


    public boolean isStopOnFirstMatch()
    {
        return stopOnFirstMatch;
    }


    public void setStopOnFirstMatch( boolean stopOnFirstMatch )
    {
        this.stopOnFirstMatch = stopOnFirstMatch;
    }


    public boolean isUnmatchedEditable()
    {
        return unmatchedEditable;
    }


    public void setUnmatchedEditable( boolean unmatchedEditable )
    {
        this.unmatchedEditable = unmatchedEditable;
    }


    @Override
    public ClassName getClassName()
    {
        return ClassName.Team;
    }
}
