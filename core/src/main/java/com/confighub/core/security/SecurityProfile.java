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
import com.confighub.core.repository.PropertyKey;
import com.confighub.core.repository.RepoFile;
import com.confighub.core.repository.Repository;
import com.confighub.core.store.APersisted;
import com.confighub.core.store.diff.SecurityProfileDiffTracker;
import com.confighub.core.utils.Utils;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.envers.AuditMappedBy;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

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
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;


@Entity
@Cacheable
@Cache( usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE )
@Table( name = "securityprofile",
        uniqueConstraints = @UniqueConstraint( columnNames = { "name",
                                                               "repositoryId" } ) )
@NamedQueries(
      {
            @NamedQuery( name = "SecurityProfile.getAll",
                         query = "SELECT s FROM SecurityProfile s WHERE repository=:repository" ),

            @NamedQuery( name = "SecurityProfile.byName",
                         query = "SELECT s FROM SecurityProfile s WHERE repository=:repository AND name=:name" )
      } )
@EntityListeners( { SecurityProfileDiffTracker.class } )
@Audited
@AuditTable( "securityprofile_audit" )
public class SecurityProfile
      extends APersisted
{
    private static final Logger log = LogManager.getLogger( SecurityProfile.class );

    @Id
    @GeneratedValue
    private Long id;

    @Column( nullable = false )
    private String name;

    @Column( nullable = false )
    private String spPassword;

    @ManyToOne( fetch = FetchType.LAZY,
                cascade = { CascadeType.REFRESH,
                            CascadeType.PERSIST } )
    @JoinColumn( nullable = false,
                 name = "repositoryId" )
    private Repository repository;

    @AuditMappedBy( mappedBy = "securityProfile" )
    @OneToMany( fetch = FetchType.LAZY,
                cascade = { CascadeType.REFRESH,
                            CascadeType.PERSIST },
                mappedBy = "securityProfile" )
    private Set<PropertyKey> keys;

    @AuditMappedBy( mappedBy = "securityProfile" )
    @OneToMany( fetch = FetchType.LAZY,
                cascade = { CascadeType.REFRESH,
                            CascadeType.PERSIST },
                mappedBy = "securityProfile" )
    private Set<RepoFile> files;

    @AuditMappedBy( mappedBy = "securityProfiles" )
    @ManyToMany( fetch = FetchType.LAZY,
                 cascade = { CascadeType.REFRESH,
                             CascadeType.PERSIST },
                 mappedBy = "securityProfiles" )
    @NotAudited
    private Set<Token> tokens;

    @Enumerated( EnumType.STRING )
    private CipherTransformation cipher;


    // --------------------------------------------------------------------------------------------
    // Construction
    // --------------------------------------------------------------------------------------------
    protected SecurityProfile()
    {
    }


    public SecurityProfile( final Repository repository,
                            final CipherTransformation cipher,
                            final String name,
                            final String spPassword )
          throws ConfigException
    {
        this.repository = repository;
        this.cipher = cipher;
        this.name = name;
        setSecret( null, spPassword );
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


    @PreRemove
    public void preRemove()
          throws ConfigException
    {
        if ( null != this.tokens && this.tokens.size() > 0 )
        {
            this.tokens.forEach( t -> {
                t.remove( this );
            } );
        }
    }


    // --------------------------------------------------------------------------------------------
    // Encode / Decode
    // --------------------------------------------------------------------------------------------
    public transient String sk;


    public String encrypt( final String text,
                           final String clearTextSecret )
          throws ConfigException
    {
        return Encryption.encrypt( this.cipher, text, clearTextSecret );
    }


    public String decrypt( final String text,
                           final String clearTextSecret )
          throws ConfigException
    {
        return Encryption.decrypt( this.cipher, text, clearTextSecret );
    }


    // --------------------------------------------------------------------------------------------
    // POJO
    // --------------------------------------------------------------------------------------------
    public boolean hasKeys()
    {
        return this.keys != null && this.keys.size() > 0;
    }


    public boolean isSecretValid( String challenge )
          throws ConfigException
    {
        return getDecodedPassword().equals( challenge );
    }


    public boolean encryptionEnabled()
    {
        return null != this.cipher;
    }


    @Override
    public boolean equals( Object o )
    {
        if ( null == o )
        {
            return false;
        }

        SecurityProfile other = (SecurityProfile) o;
        return this.getId().equals( other.getId() );
    }


    @Override
    public int hashCode()
    {
        return Objects.hash( name, this.getName(), this.getId(), repository.getName() );
    }


    public JsonObject toJson()
    {
        JsonObject json = new JsonObject();
        json.addProperty( "id", this.id );
        json.addProperty( "name", this.name );
        json.addProperty( "cipher", null == this.cipher ? null : this.cipher.getName() );

        return json;
    }


    // --------------------------------------------------------------------------------------------
    // Setters and getters
    // --------------------------------------------------------------------------------------------
    public Long getId()
    {
        return id;
    }


    public String getName()
    {
        return name;
    }


    public void setName( String name )
    {
        this.name = name;
    }


    public String getSpPassword()
    {
        return spPassword;
    }


    public String getDecodedPassword()
          throws ConfigException
    {
        return Encryption.decrypt( Auth.internalCipher, this.spPassword, Auth.getSecurityGroupPassword() );
    }


    public Set<RepoFile> getFiles()
    {
        return this.files;
    }


    public void setSecret( String oldPass,
                           String secret )
          throws ConfigException
    {
        if ( null != this.spPassword )
        {
            if ( !isSecretValid( oldPass ) )
            {
                throw new ConfigException( Error.Code.INVALID_PASSWORD );
            }
        }

        if ( !Utils.passwordRequirementsSatisfied( secret ) )
        {
            throw new ConfigException( Error.Code.PASSWORD_REQUIREMENTS );
        }

        this.spPassword = Encryption.encrypt( Auth.internalCipher, secret, Auth.getSecurityGroupPassword() );
    }


    public Repository getRepository()
    {
        return repository;
    }


    public Set<PropertyKey> getKeys()
    {
        if ( null == this.keys )
        {
            return new HashSet<>();
        }

        return keys;
    }


    public void setKeys( Set<PropertyKey> keys )
    {
        this.keys = keys;
    }


    public CipherTransformation getCipher()
    {
        return cipher;
    }


    public void setCipher( CipherTransformation cipher )
    {
        this.cipher = cipher;
    }


    public Set<Token> getTokens()
    {
        return tokens;
    }


    @Override
    public ClassName getClassName()
    {
        return ClassName.SecurityProfile;
    }
}
