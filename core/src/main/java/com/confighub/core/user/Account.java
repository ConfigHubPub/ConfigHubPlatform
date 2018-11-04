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

package com.confighub.core.user;

import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.organization.Organization;
import com.confighub.core.repository.Repository;
import com.confighub.core.store.APersisted;
import com.confighub.core.utils.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;


@Entity
@Table( name = "account" )
@Cacheable
@Cache( usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE )
@NamedQueries(
      {
            @NamedQuery( name = "AccountName.get",
                         query = "SELECT p from Account p WHERE name=:name" ),
            @NamedQuery( name = "AccountName.count",
                         query = "SELECT COUNT(p.name) from Account p WHERE name=:name" )
      } )
public class Account
      extends APersisted
{
    private static final Logger log = LogManager.getLogger( Account.class );

    @Id
    @GeneratedValue
    private Long id;

    @Column( nullable = false,
             unique = true )
    private String name;

    @OneToOne( fetch = FetchType.LAZY,
               cascade = { CascadeType.REFRESH } )
    private UserAccount user;

    @OneToOne( fetch = FetchType.LAZY,
               cascade = { CascadeType.REFRESH } )
    private Organization organization;

    @Column( name = "company" )
    private String company;

    @Column( name = "website" )
    private String website;

    @Column( name = "city" )
    private String city;

    @Column( name = "country" )
    private String country;

    @ManyToMany( fetch = FetchType.LAZY,
                 cascade = { CascadeType.REFRESH,
                             CascadeType.PERSIST } )
    private Set<Repository> repositories;

    // -------------------------------------------------------------------------------------------- //
    // POJO Operations
    // -------------------------------------------------------------------------------------------- //


    public boolean isPersonal()
    {
        return null != this.user;
    }


    @PostUpdate
    @PostPersist
    public void validateName()
          throws ConfigException
    {
        if ( !Utils.isNameValid( this.name ) )
        {
            throw new ConfigException( Error.Code.ILLEGAL_CHARACTERS );
        }
    }


    @Override
    public boolean equals( Object other )
    {
        if ( null == other )
        {
            return false;
        }

        if ( !( other instanceof Account ) )
        {
            return false;
        }

        Account o = (Account) other;
        return this.name.equals( o.getName() );
    }

    // --------------------------------------------------------------------------------------------
    // Setters and getters
    // --------------------------------------------------------------------------------------------


    @Override
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


    public UserAccount getUser()
    {
        return user;
    }


    public void setUser( UserAccount user )
    {
        this.user = user;
    }


    public Organization getOrganization()
    {
        return organization;
    }


    public void setOrganization( Organization organization )
    {
        this.organization = organization;
    }


    public String getCompany()
    {
        return company;
    }


    public void setCompany( String company )
    {
        this.company = company;
    }


    public String getWebsite()
    {
        return website;
    }


    public void setWebsite( String website )
    {
        this.website = website;
    }


    public String getCity()
    {
        return city;
    }


    public void setCity( String city )
    {
        this.city = city;
    }


    public String getCountry()
    {
        return country;
    }


    public void setCountry( String country )
    {
        this.country = country;
    }


    @Override
    public String toString()
    {
        return String.format( "Account [%d] %s", this.id, this.name );
    }

    // --------------------------------------------------------------------------------------------
    // Repository management
    // --------------------------------------------------------------------------------------------


    public void joinRepository( final Repository repository )
    {
        if ( null == this.repositories )
        {
            this.repositories = new HashSet<>();
        }

        this.repositories.add( repository );
    }


    public boolean removeRepository( final Repository repository )
    {
        return null != this.repositories && this.repositories.remove( repository );
    }


    public int getRepositoryCount()
    {
        if ( null == this.repositories )
        {
            return 0;
        }

        return this.repositories.size();
    }


    public Set<Repository> getRepositories()
    {
        return repositories;
    }


    @Override
    public ClassName getClassName()
    {
        return ClassName.Account;
    }
}
