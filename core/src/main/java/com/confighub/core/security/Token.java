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

package com.confighub.core.security;

import com.confighub.core.auth.Auth;
import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.organization.Team;
import com.confighub.core.repository.Repository;
import com.confighub.core.rules.AccessRuleWrapper;
import com.confighub.core.store.APersisted;
import com.confighub.core.store.diff.TokenDiffTracker;
import com.confighub.core.user.UserAccount;
import com.confighub.core.utils.Utils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;

import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;


@Entity
@Table( name = "token" )
@Cacheable
@Cache( usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE )
@NamedQueries(
      {
            @NamedQuery( name = "Token.getAll",
                         query = "SELECT t FROM Token t WHERE repository=:repository" ),
            @NamedQuery( name = "Token.getToken",
                         query = "SELECT t FROM Token t WHERE token=:token AND repository=:repository" ),
            @NamedQuery( name = "Token.byId",
                         query = "SELECT t FROM Token t WHERE repository=:repository AND id=:id" ),
      } )
@EntityListeners( { TokenDiffTracker.class } )
@Audited
@AuditTable( "token_audit" )
public class Token
      extends APersisted
{
    private static final Logger log = LogManager.getLogger( Token.class );

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne( fetch = FetchType.LAZY,
                cascade = { CascadeType.REFRESH,
                            CascadeType.PERSIST } )
    @JoinColumn( nullable = false )
    private Repository repository;

    @NotAudited
    @Column( length = 450 )
    private String token;

    @Column( name = "name" )
    private String name;

    @Column( name = "active" )
    private boolean active;

    @Column( name = "expires" )
    private Long expires;

    @Column( name = "forceKeyPushEnabled" )
    private boolean forceKeyPushEnabled = false;

    @Column( name = "createdOn" )
    private Long createdOn;

    @ManyToMany( fetch = FetchType.LAZY,
                 cascade = { CascadeType.REFRESH } )
    private Set<SecurityProfile> securityProfiles;

    @ManyToOne( fetch = FetchType.LAZY,
                cascade = { CascadeType.REFRESH,
                            CascadeType.PERSIST } )
    @JoinColumn( name = "teamRulesId" )
    @Audited( targetAuditMode = RelationTargetAuditMode.NOT_AUDITED )
    private Team teamRules;

    @ManyToOne( fetch = FetchType.LAZY,
                cascade = { CascadeType.REFRESH,
                            CascadeType.PERSIST } )
    @JoinColumn( name = "managingTeamId" )
    @Audited( targetAuditMode = RelationTargetAuditMode.NOT_AUDITED )
    private Team managingTeam;

    @ManyToOne( fetch = FetchType.LAZY,
                cascade = { CascadeType.REFRESH,
                            CascadeType.PERSIST } )
    @JoinColumn( name = "userAccountId" )
    @Audited( targetAuditMode = RelationTargetAuditMode.NOT_AUDITED )
    private UserAccount user;

    @Enumerated( EnumType.STRING )
    @Column( nullable = false,
             name = "managedBy" )
    private ManagedBy managedBy;

    public enum ManagedBy
    {
        User, Admins, Team, All
    }


    protected Token()
    {
    }


    /**
     * Create API Token
     *
     * @param repository
     * @param name
     * @param expiration
     * @param forceKeyPushEnabled
     * @param securityProfiles
     * @param teamRules
     * @param managingTeam
     * @param userAccount
     * @param managedBy
     */
    public Token( final Repository repository,
                  final String name,
                  final Long expiration,
                  final boolean forceKeyPushEnabled,
                  final List<SecurityProfile> securityProfiles,
                  final String teamRules,
                  final String managingTeam,
                  final UserAccount userAccount,
                  final ManagedBy managedBy )
    {
        this.name = name;
        this.expires = expiration;
        this.repository = repository;
        this.createdOn = ( new Date() ).getTime();
        this.forceKeyPushEnabled = forceKeyPushEnabled;
        this.active = true;
        this.managedBy = managedBy;

        if ( null != securityProfiles )
        {
            for ( SecurityProfile sp : securityProfiles )
            {
                this.addSecurityProfile( sp );
            }
        }

        if ( null != teamRules )
        {
            Team team = repository.getTeam( teamRules );
            if ( null == team )
            {
                throw new ConfigException( Error.Code.TEAM_NOT_FOUND );
            }

            this.teamRules = team;
        }

        switch ( managedBy )
        {
            case User:
                if ( null == userAccount )
                {
                    throw new ConfigException( Error.Code.MISSING_PARAMS );
                }
                this.user = userAccount;
                this.teamRules = repository.getTeamForUser( userAccount );
                break;

            case Team:
                this.user = null;
                Team team = repository.getTeam( managingTeam );
                if ( null == team )
                {
                    throw new ConfigException( Error.Code.TEAM_NOT_FOUND );
                }
                this.managingTeam = team;
                break;

            case Admins:
            case All:
                this.user = null;
                this.managingTeam = null;
                break;
        }

        this.build();
    }



    /**
     * API Token can only be updated from this method.
     *
     * @param active
     * @param name
     * @param expiration
     * @param forceKeyPushEnabled
     * @param addSecurityProfiles
     * @param removeSecurityProfiles
     * @param teamRules
     * @param managingTeam
     * @param managedBy
     * @param newOwner
     * @param modificationAuthor
     */
    public void updateToken( final boolean active,
                             final String name,
                             final Long expiration,
                             final boolean forceKeyPushEnabled,
                             final List<SecurityProfile> addSecurityProfiles,
                             final List<SecurityProfile> removeSecurityProfiles,
                             final String teamRules,
                             final String managingTeam,
                             final ManagedBy managedBy,
                             final String newOwner,
                             final UserAccount modificationAuthor )
          throws ConfigException
    {
        boolean canEdit = isAllowedToEdit( modificationAuthor );

        if ( null != addSecurityProfiles )
        {
            for ( SecurityProfile sp : addSecurityProfiles )
            {
                this.addSecurityProfile( sp );
            }
        }

        if ( null != removeSecurityProfiles )
        {
            for ( SecurityProfile sp : removeSecurityProfiles )
            {
                this.remove( sp );
            }
        }

        if ( canEdit )
        {
            this.active = active;

            this.name = name;
            this.expires = expiration;
            this.forceKeyPushEnabled = forceKeyPushEnabled;
            this.managedBy = managedBy;

            if ( !Utils.isBlank( teamRules ) )
            {
                Team team = repository.getTeam( teamRules );
                if ( null == team )
                {
                    throw new ConfigException( Error.Code.TEAM_NOT_FOUND );
                }

                this.teamRules = team;
            }
            else
            {
                this.teamRules = null;
            }

            switch ( managedBy )
            {
                case User:
                    if ( null == modificationAuthor )
                    {
                        throw new ConfigException( Error.Code.MISSING_PARAMS );
                    }

                    if ( modificationAuthor.getUsername().equals( newOwner ) )
                    {
                        this.user = modificationAuthor;

                        // If not an admin, a user has to have personal token assigned to their team.
                        if ( !this.repository.isAdminOrOwner( modificationAuthor ) )
                        {
                            this.teamRules = repository.getTeamForUser( modificationAuthor );
                        }

                        this.managingTeam = null;
                    }

                    break;

                case Team:
                    this.user = null;
                    Team team = repository.getTeam( managingTeam );
                    if ( null == team )
                    {
                        throw new ConfigException( Error.Code.TEAM_NOT_FOUND );
                    }
                    this.managingTeam = team;
                    break;

                case Admins:
                case All:
                    this.user = null;
                    this.managingTeam = null;
                    break;
            }
        }
    }


    public boolean isAllowedToDelete( UserAccount user )
    {
        if ( null == user )
        {
            return false;
        }

        return this.repository.isAdminOrOwner( user )
               || ( ManagedBy.User.equals( this.managedBy ) && user.equals( this.user ) );
    }


    public boolean isAllowedToEdit( UserAccount user )
    {
        if ( null == user )
        {
            return false;
        }

        if ( this.repository.isAdminOrOwner( user ) )
        {
            return true;
        }

        switch ( this.managedBy )
        {
            // user is the token owner
            case User:
                return user.equals( this.user );

            // user is a member of a managing team
            case Team:
                return null != this.managingTeam
                       && this.managingTeam.isMember( user );

            case Admins:
                return false;

            case All:
                return false;
        }

        return false;
    }


    public boolean isAllowedToViewToken( UserAccount user )
    {
        if ( null == user )
        {
            return false;
        }

        switch ( this.managedBy )
        {
            // user is the token owner
            case User:
                return user.equals( this.user );

            // user is a member of a managing team
            case Team:
                return null != this.managingTeam
                       && this.managingTeam.isMember( user );

            case Admins:
                return this.repository.isAdminOrOwner( user );

            case All:
                return true;
        }

        return false;
    }


    @Override
    public String toString()
    {
        return String.format( "Token[%5d]: %s, repoId: %d", this.id, this.name, this.repository.getId() );
    }


    public JsonObject toJson()
    {
        JsonObject json = new JsonObject();

        json.addProperty( "name", this.name );
        json.addProperty( "expires", this.expires );
        json.addProperty( "forceKeyPushEnabled", this.forceKeyPushEnabled );

        if ( null != this.securityProfiles )
        {
            JsonArray sps = new JsonArray();
            for ( SecurityProfile sp : this.securityProfiles )
            {
                sps.add( sp.getName() );
            }
            json.add( "sps", sps );
        }

        return json;
    }


    @PreUpdate
    @PrePersist
    public void enforce()
          throws ConfigException
    {
        if ( Utils.isBlank( this.name ) )
        {
            throw new ConfigException( Error.Code.BLANK_NAME );
        }

        if ( !Utils.isNameValid( this.name ) )
        {
            throw new ConfigException( Error.Code.ILLEGAL_CHARACTERS );
        }
    }


    public AccessRuleWrapper getRulesWrapper()
    {
        if ( null != teamRules )
        {
            return this.repository.getRulesWrapper( this.teamRules );
        }

        return null;
    }


    public void clearSecurityProfiles()
    {
        this.securityProfiles = null;
    }


    /**
     * @param sp
     */
    private void addSecurityProfile( SecurityProfile sp )
    {
        if ( null == this.securityProfiles )
        {
            this.securityProfiles = new HashSet<>();
        }

        this.securityProfiles.add( sp );
    }


    /**
     * @param sp
     */
    protected void remove( SecurityProfile sp )
    {
        if ( null != this.securityProfiles )
        {
            this.securityProfiles.remove( sp );
        }
    }


    private void build()
          throws ConfigException
    {
        if ( null == this.token )
        {
            this.token = generateToken();
        }
    }


    public boolean isExpired()
    {
        if ( null == this.expires )
        {
            return false;
        }

        return this.expires < System.currentTimeMillis();
    }


    private String generateToken()
    {
        return Auth.getApiToken( this.repository );
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

        return o instanceof Token && ( (Token) o ).getId().equals( this.getId() );
    }


    @Override
    public int hashCode()
    {
        return Objects.hash( this.token );
    }


    // --------------------------------------------------------------------------------------------
    // Setters and getters
    // --------------------------------------------------------------------------------------------
    @Override
    public Long getId()
    {
        return id;
    }


    public Repository getRepository()
    {
        return repository;
    }


    public String getToken()
    {
        return token;
    }


    public String getName()
    {
        return name;
    }


    public boolean isForceKeyPushEnabled()
    {
        return forceKeyPushEnabled;
    }


    public boolean isActive()
    {
        return active;
    }


    public Long getExpires()
    {
        return expires;
    }


    public Long getCreatedOn()
    {
        return createdOn;
    }


    public Set<SecurityProfile> getSecurityProfiles()
    {
        return securityProfiles;
    }


    public Team getTeamRules()
    {
        return teamRules;
    }


    public Team getManagingTeam()
    {
        return managingTeam;
    }


    public ManagedBy getManagedBy()
    {
        return managedBy;
    }


    public UserAccount getUser()
    {
        return user;
    }


    @Override
    public ClassName getClassName()
    {
        return ClassName.Token;
    }
}
