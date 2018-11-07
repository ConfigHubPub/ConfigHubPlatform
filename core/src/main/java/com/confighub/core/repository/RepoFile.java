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

package com.confighub.core.repository;

import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.resolver.Context;
import com.confighub.core.security.SecurityProfile;
import com.confighub.core.store.diff.RepoFileDiffTracker;
import com.confighub.core.utils.FileUtils;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.*;


@Entity
@Table( name = "repofile" )
@Cacheable
@org.hibernate.annotations.Cache( usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE )
@NamedQueries(
      {
            @NamedQuery( name = "RepoFile.count",
                         query = "SELECT COUNT(c) FROM RepoFile c" ),
            @NamedQuery( name = "RepoFile.search",
                         query = "SELECT f FROM RepoFile f WHERE repository=:repository AND " +
                                 "(absFilePath.absPath LIKE :searchTerm OR content LIKE :searchTerm)" )
      } )
@Audited
@AuditTable( "repofile_audit" )
@EntityListeners( { RepoFileDiffTracker.class } )
public class RepoFile
      extends AContextAwarePersistent
{
    private static final Logger log = LogManager.getLogger( RepoFile.class );

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne( fetch = FetchType.LAZY,
                cascade = { CascadeType.PERSIST,
                            CascadeType.REFRESH } )
    @JoinColumn( nullable = false )
    private AbsoluteFilePath absFilePath;

    @Lob
    @Type( type = "org.hibernate.type.TextType" )
    @Column( nullable = false,
             columnDefinition = "TEXT" )
    private String content;

    @ManyToOne( fetch = FetchType.LAZY,
                cascade = { CascadeType.REFRESH,
                            CascadeType.PERSIST } )
    private SecurityProfile securityProfile;

    @ManyToMany( fetch = FetchType.LAZY,
                 cascade = { CascadeType.REFRESH,
                             CascadeType.PERSIST } )
    private Set<PropertyKey> keys;


    // --------------------------------------------------------------------------------------------
    // Construction
    // --------------------------------------------------------------------------------------------


    protected RepoFile()
    {
    }


    public RepoFile( final Repository repository,
                     final AbsoluteFilePath absFilePath,
                     final String content,
                     final Set<CtxLevel> context )
    {
        this.repository = repository;
        this.absFilePath = absFilePath;
        this.content = content;
        this.context = context;
        this.absFilePath.addFile( this );

        updateContextString();
    }


    // --------------------------------------------------------------------------------------------
    // Validation before saving
    // --------------------------------------------------------------------------------------------
    @PreRemove
    public void preRemove()
    {
        this.absFilePath.removeFile( this );
    }


    @PreUpdate
    @PrePersist
    public void enforce()
          throws ConfigException
    {

        // Inactive properties can exist without any restrictions
        if ( !this.active )
        {
            return;
        }

        // 1. Path cannot be blank
        if ( null == this.absFilePath )
        {
            throw new ConfigException( Error.Code.BLANK_NAME );
        }

        // 2. Context cannot contain more than one level with the same depth
        validateContext();

        // 3. AbsolutePath with the same context signature cannot exist
        for ( RepoFile file : absFilePath.getFiles() )
        {
            if ( file.equals( this ) || !file.isActive() )
            {
                continue;
            }

            if ( file.getContextWeight() == this.getContextWeight() )
            {
                if ( CollectionUtils.isEqualCollection( this.context, file.getContext() ) )
                {
                    throw new ConfigException( Error.Code.FILE_DUPLICATION_CONTEXT );
                }
            }
        }

        // 4. Cross-cluster conflict detection
        if ( this.repository.isContextClustersEnabled() &&
             null != this.context &&
             this.absFilePath.getFiles().size() > 1 )
        {

            // Check if the key for this property has values with the same context weight, as this is
            // the first indicator of a potential cross-cluster conflict.
            List<AContextAwarePersistent> conflictCandidates = null;
            for ( RepoFile file : this.absFilePath.getFiles() )
            {
                if ( file.equals( this ) )
                {
                    continue;
                }

                if ( this.contextWeight == file.getContextWeight() )
                {
                    if ( null == conflictCandidates )
                    {
                        conflictCandidates = new ArrayList<>();
                    }
                    conflictCandidates.add( file );
                }
            }

            crossClusterValidation( conflictCandidates );
        }

        if ( this.active && null != this.keys && this.keys.size() > 0 )
        {
            Context context = new Context( null, this.repository, this.getContext(), null );
            checkFileForCircularReference( context, this );
        }
    }


    private void checkFileForCircularReference( final Context context,
                                                final RepoFile file )
          throws ConfigException
    {
        Map<PropertyKey, Collection<Property>> keyListMap = context.resolveFile( file.getKeys(), false );
        HashMap<RepoFile, Property> breadcrumbs = new LinkedHashMap<>();

        for ( PropertyKey key : keyListMap.keySet() )
        {
            if ( !PropertyKey.ValueDataType.FileEmbed.equals( key.getValueDataType() ) )
            {
                continue;
            }

            for ( Property property : keyListMap.get( key ) )
            {
                breadcrumbs.clear();
                checkPropertyCircularReference( context, property, breadcrumbs );
            }
        }
    }


    // --------------------------------------------------------------------------------------------
    // AbsoluteFilePath management
    // --------------------------------------------------------------------------------------------


    public String getAbsPath()
    {
        return this.absFilePath.getAbsPath();
    }


    public Set<RepoFile> getFiles()
    {
        return this.absFilePath.getFiles();
    }


    public Long getAbsPathId()
    {
        return this.absFilePath.getId();
    }

    // --------------------------------------------------------------------------------------------
    // POJO Ops
    // --------------------------------------------------------------------------------------------


    @Override
    public String toString()
    {
        return String.format( "[%s] RepoFile[%5d]: %s | context[%3d]: %s",
                              this.revType,
                              this.id,
                              this.getAbsPath(),
                              this.getContextWeight(),
                              getContextJson() );
    }


    @Override
    public int hashCode()
    {
        // Because envers will store deleted item without any data, we have to be ready for this.
        if ( Utils.anyNull( repository, this.absFilePath ) )
        {
            return this.id.intValue();
        }

        return Objects.hash( this.repository.getName(),
                             this.absFilePath.getAbsPath(),
                             this.contextWeight,
                             this.contextJson );
    }


    @Override
    public boolean equals( Object o )
    {
        if ( null == o || !( o instanceof RepoFile ) )
        {
            return false;
        }
        RepoFile other = (RepoFile) o;

        return Utils.same( this.getContextJson(), other.getContextJson() ) &&
               this.isActive() == other.isActive() &&
               this.absFilePath.equals( other.absFilePath ) &&
               Utils.equal( this.id, other.id );
    }


    @Override
    public JsonObject toJson()
    {
        JsonObject json = new JsonObject();
        json.addProperty( "id", this.id );
        if ( null != this.securityProfile )
        {
            json.addProperty( "spName", this.securityProfile.getName() );
        }

        Gson gson = new Gson();
        json.add( "levels", gson.fromJson( this.contextJson, JsonArray.class ) );
        json.addProperty( "active", this.isActive() );
        json.addProperty( "score", this.getContextWeight() );

        return json;
    }

    // --------------------------------------------------------------------------------------------
    // Security
    // --------------------------------------------------------------------------------------------


    public void decryptFile( final String encryptionSecret )
          throws ConfigException
    {
        if ( !this.isEncrypted() || decrypted )
        {
            return;
        }
        this.content = this.securityProfile.decrypt( this.content, encryptionSecret );
        this.decrypted = true;
    }


    public void encryptFile( final String encryptionSecret )
          throws ConfigException
    {
        if ( !this.isEncrypted() )
        {
            return;
        }
        this.content = this.securityProfile.encrypt( this.content, encryptionSecret );
        this.decrypted = false;
    }


    public void updateKey( final PropertyKey oldKey,
                           final String newKeyString,
                           final PropertyKey newKey )
          throws ConfigException
    {
        if ( this.isEncrypted() )
        {
            String pass = this.securityProfile.getDecodedPassword();
            decryptFile( pass );
            this.content = FileUtils.replaceKey( this.content, oldKey.getKey(), newKeyString );
            encryptFile( pass );
        }
        else
        {
            this.content = FileUtils.replaceKey( this.content, oldKey.getKey(), newKeyString );
        }

        if ( null != newKey )
        {
            this.keys.remove( oldKey );
            this.keys.add( newKey );
        }
    }


    public String getContent()
    {
        return content;
    }


    public void setContent( String content )
          throws ConfigException
    {
        setContent( content, null );
    }


    public Set<PropertyKey> getKeys()
    {
        return keys;
    }


    public void setKeys( Set<PropertyKey> keys )
    {
        this.keys = keys;
    }


    public SecurityProfile getSecurityProfile()
    {
        return securityProfile;
    }


    private transient boolean decrypted = false;


    public void setContent( final String content,
                            final String encryptionSecret )
          throws ConfigException
    {
        if ( !this.isSecure() )
        {
            this.content = content;
            return;
        }

        if ( this.repository.isSecurityProfilesEnabled() )
        {
            if ( !this.securityProfile.isSecretValid( encryptionSecret ) )
            {
                throw new ConfigException( Error.Code.INVALID_PASSWORD );
            }

            if ( this.isEncrypted() )
            {
                this.content = this.securityProfile.encrypt( content, encryptionSecret );
            }
            else
            {
                this.content = content;
            }
        }
        else
        {
            this.content = content;
        }
    }


    public void setAbsFilePath( AbsoluteFilePath absFilePath )
    {
        if ( null == absFilePath )
        {
            return;
        }

        if ( null != this.absFilePath )
        {
            if ( absFilePath.equals( this.absFilePath ) )
            {
                return;
            }

            this.absFilePath.removeFile( this );
        }

        this.absFilePath = absFilePath;
        this.absFilePath.addFile( this );
    }


    /**
     * @param securityProfile
     * @param password
     * @throws ConfigException
     */
    public void setSecurityProfile( final SecurityProfile securityProfile,
                                    final String password )
          throws ConfigException
    {
        if ( null != this.securityProfile )
        {
            decryptFile( password );
        }

        this.securityProfile = securityProfile;
        setContent( this.content, securityProfile.sk );
    }


    /**
     * @param existingSecretKey
     * @throws ConfigException
     */
    public void removeSecurityProfile( final String existingSecretKey )
          throws ConfigException
    {
        if ( null != this.securityProfile )
        {
            decryptFile( existingSecretKey );
        }

        this.securityProfile = null;
    }


    public boolean isSecure()
    {
        return null != this.securityProfile;
    }


    public boolean isEncrypted()
    {
        return null != this.securityProfile && this.securityProfile.encryptionEnabled();
    }


    public AbsoluteFilePath getAbsFilePath()
    {
        return absFilePath;
    }


    @Override
    public Long getId()
    {
        return this.id;
    }


    @Override
    public ClassName getClassName()
    {
        return ClassName.RepoFile;
    }
}
