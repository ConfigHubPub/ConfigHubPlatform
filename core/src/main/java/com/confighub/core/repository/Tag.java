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
import com.confighub.core.store.APersisted;
import com.confighub.core.store.diff.TagDiffTracker;
import com.confighub.core.utils.DateTimeUtils;
import com.confighub.core.utils.Utils;
import com.google.gson.JsonObject;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.Date;


@Entity
@NamedQueries(
      {
            @NamedQuery( name = "Tag.getByName",
                         query = "SELECT t FROM Tag t WHERE repository.id=:repositoryId AND name=:name" ),
            @NamedQuery( name = "Tag.getAll",
                         query = "SELECT t FROM Tag t WHERE repository=:repository" ),
      } )
@Cacheable
@Cache( usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE )
@Table( name = "tag",
        uniqueConstraints = @UniqueConstraint( columnNames = { "name",
                                                               "repositoryId" } ) )
@Audited
@AuditTable( "tag_audit" )
@EntityListeners( { TagDiffTracker.class } )
public class Tag
      extends APersisted
{
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne( fetch = FetchType.LAZY,
                cascade = { CascadeType.REFRESH } )
    @JoinColumn( nullable = false,
                 name = "repositoryId" )
    private Repository repository;

    @Column( name = "readme" )
    private String readme;

    @Column( nullable = false )
    private String name;

    @Column( nullable = false )
    private Long ts;


    protected Tag()
    {
    }


    public Tag( final Repository repository )
    {
        this.repository = repository;
    }


    @Override
    public String toString()
    {
        return String.format( "Tag[%d] %s | %d", this.id, this.name, this.ts );
    }


    public JsonObject toJson()
    {
        JsonObject json = new JsonObject();
        json.addProperty( "name", this.name );
        json.addProperty( "readme", null == this.readme ? "" : this.readme );
        json.addProperty( "ts", this.ts );
        json.addProperty( "date", DateTimeUtils.toISO8601( new Date( this.ts ) ) );


        return json;
    }

    // --------------------------------------------------------------------------------------------
    // Setters and getters
    // --------------------------------------------------------------------------------------------


    @Override
    public Long getId()
    {
        return id;
    }


    public String getReadme()
    {
        return readme;
    }


    public void setReadme( String readme )
    {
        this.readme = readme;
    }


    public String getName()
    {
        return name;
    }


    public void setName( String name )
          throws ConfigException
    {
        if ( !Utils.isKeyValid( name ) )
        {
            throw new ConfigException( Error.Code.ILLEGAL_CHARACTERS );
        }

        this.name = name;
    }


    public Long getTs()
    {
        return ts;
    }


    public void setTs( Long ts )
    {
        this.ts = ts;
    }


    @Override
    public ClassName getClassName()
    {
        return ClassName.Tag;
    }
}
