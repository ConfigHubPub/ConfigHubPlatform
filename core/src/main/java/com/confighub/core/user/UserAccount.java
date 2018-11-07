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
import com.confighub.core.security.Token;
import com.confighub.core.store.APersisted;
import com.confighub.core.store.diff.UserDiffTracker;
import com.confighub.core.utils.Passwords;
import com.confighub.core.utils.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
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
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;


@Entity
@Table( name = "useraccount" )
@Cacheable
@Cache( usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE )
@NamedQueries(
      {
            @NamedQuery( name = "User.count",
                         query = "SELECT COUNT(c) FROM UserAccount c WHERE c.active=true" ),
            @NamedQuery( name = "User.loginByUsername",
                         query = "SELECT u FROM UserAccount u WHERE u.account.name=:username" ),
            @NamedQuery( name = "User.loginByEmail",
                         query = "SELECT u FROM UserAccount u WHERE email.email=:email " ),
            @NamedQuery( name = "User.isRegistered",
                         query = "SELECT COUNT(u.email.email) FROM UserAccount u WHERE email.email=:email" ),
            @NamedQuery( name = "User.isUsernameTaken",
                         query = "SELECT COUNT(u.account.name) FROM UserAccount u WHERE account.name=:username" ),
            @NamedQuery( name = "User.getByUsername",
                         query = "SELECT u FROM UserAccount u WHERE u.account.name=:username" ),
            @NamedQuery( name = "Users.search",
                         query = "SELECT u FROM UserAccount u WHERE u.account.name LIKE :searchTerm OR u.name LIKE :searchTerm" ),
            @NamedQuery( name = "Users.sysAdmins",
                         query = "SELECT u FROM UserAccount u WHERE u.configHubAdmin=true " )
      } )
@EntityListeners( { UserDiffTracker.class } )
public class UserAccount
      extends APersisted
{
    private static final Logger log = LogManager.getLogger( UserAccount.class );

    public enum AccountType
    {
        LOCAL,
        LDAP
    }

    @Id
    @GeneratedValue
    private Long id;

    @JoinColumn( nullable = false,
                 unique = true )
    @OneToOne( fetch = FetchType.LAZY,
               cascade = { CascadeType.ALL } )
    private Email email;

    @JoinColumn( nullable = false,
                 unique = true )
    @OneToOne( fetch = FetchType.LAZY,
               cascade = { CascadeType.ALL } )
    private Account account;

    private String userPassword;

    @Column( name = "name" )
    private String name;

    @ManyToMany( fetch = FetchType.LAZY,
                 cascade = { CascadeType.REFRESH } )
    private Set<Organization> organizations;

    @Enumerated( EnumType.STRING )
    @Column( name = "account_type",
             nullable = false )
    private AccountType accountType;

    @Column( nullable = false )
    private Date createDate;

    @Column( name = "timezone" )
    private String timezone;

    @Column( name = "active" )
    private boolean active;

    @Column( name = "configHubAdmin" )
    private boolean configHubAdmin = false;

    // Notification preferences
    @Column( name = "emailRepoCritical" )
    private boolean emailRepoCritical = true;

    @Column( name = "emailBlog" )
    private boolean emailBlog = true;

    @ManyToMany( fetch = FetchType.LAZY,
                 cascade = { CascadeType.REFRESH,
                             CascadeType.PERSIST } )
    @NotAudited
    private Set<Token> tokens;


    @Override
    public String toString()
    {
        return String.format( "%d | %s | %s | [%s]",
                              this.id,
                              this.email.getEmail(),
                              this.name,
                              this.active ? "active" : "INACTIVE" );
    }

    // -------------------------------------------------------------------------------------------- //
    // Validation before saving
    // -------------------------------------------------------------------------------------------- //


    @PrePersist
    protected void setCreationDate()
          throws ConfigException
    {
        this.createDate = new Date();
        enforce();
    }


    /**
     * Requirements for user:
     * 1. unique username
     * 2. userPassword specified and satisfies requirements
     */
    @PreUpdate
    public void enforce()
          throws ConfigException
    {
        if ( AccountType.LOCAL.equals( this.accountType ) &&
             Utils.anyBlank( this.userPassword ) )
        {
            throw new ConfigException( Error.Code.USER_AUTH );
        }

        // ToDo define and enforce userPassword requirements
    }

    // -------------------------------------------------------------------------------------------- //
    // Organizations
    // -------------------------------------------------------------------------------------------- //


    public void joinOrganization( final Organization organization )
    {
        if ( null == organization )
        {
            return;
        }

        if ( null == this.organizations )
        {
            this.organizations = new HashSet<>();
        }

        this.organizations.add( organization );
    }


    public void removeOrganization( final Organization organization )
    {
        if ( null == organization )
        {
            return;
        }
        if ( null == this.organizations )
        {
            return;
        }

        this.organizations.remove( organization );
    }


    public int getOrganizationCount()
    {
        if ( null == this.organizations )
        {
            return 0;
        }

        return this.organizations.size();
    }

    // -------------------------------------------------------------------------------------------- //
    // POJO Operations
    // -------------------------------------------------------------------------------------------- //


    @Override
    public boolean equals( Object other )
    {
        if ( null == other )
        {
            return false;
        }

        if ( !( other instanceof UserAccount ) )
        {
            return false;
        }

        UserAccount o = (UserAccount) other;
        return this.getEmail().equals( o.getEmail() );
    }

    // -------------------------------------------------------------------------------------------- //
    // Setters and getters
    // -------------------------------------------------------------------------------------------- //


    public Long getId()
    {
        return id;
    }


    public String getEmail()
    {
        return email.getEmail();
    }


    public void setEmail( String email )
    {
        if ( null == this.email )
        {
            this.email = new Email( email, this );
        }
        else
        {
            this.email.setEmail( email );
        }
    }


    public void setUserPassword( String userPassword )
          throws ConfigException
    {
        if ( !AccountType.LOCAL.equals( this.accountType ) )
        {
            return;
        }

        if ( !Utils.passwordRequirementsSatisfied( userPassword ) )
        {
            throw new ConfigException( Error.Code.PASSWORD_REQUIREMENTS );
        }
        this.userPassword = Passwords.generateStrongPasswordHash( userPassword );
    }


    public boolean isPasswordValid( String challenge )
          throws ConfigException
    {
        return Passwords.validatePassword( challenge, this.userPassword );
    }


    public AccountType getAccountType()
    {
        return accountType;
    }


    public void setAccountType( final AccountType accountType )
    {
        this.accountType = accountType;
    }


    public String getName()
    {
        return name;
    }


    public void setName( String name )
    {
        this.name = name;
    }


    public Set<Repository> getRepositories()
    {
        return this.account.getRepositories();
    }


    public int getRepositoryCount()
    {
        return this.account.getRepositoryCount();
    }


    public Account getAccount()
    {
        return this.account;
    }


    public String getUsername()
    {
        return account.getName();
    }


    public void setUsername( String username )
    {
        if ( null == this.account )
        {
            this.account = new Account();
            this.account.setUser( this );
        }

        this.account.setName( username );
    }


    public Date getCreateDate()
    {
        return createDate;
    }


    public Set<Organization> getOrganizations()
    {
        return organizations;
    }


    public boolean isActive()
    {
        return active;
    }


    public void setActive( boolean active )
    {
        this.active = active;
    }


    public boolean isConfigHubAdmin()
    {
        return configHubAdmin;
    }


    public void setConfigHubAdmin( boolean configHubAdmin )
    {
        this.configHubAdmin = configHubAdmin;
    }


    public boolean isEmailRepoCritical()
    {
        return emailRepoCritical;
    }


    public void setEmailRepoCritical( boolean emailRepoCritical )
    {
        this.emailRepoCritical = emailRepoCritical;
    }


    public boolean isEmailBlog()
    {
        return emailBlog;
    }


    public void setEmailBlog( boolean emailBlog )
    {
        this.emailBlog = emailBlog;
    }


    public String getTimezone()
    {
        return timezone;
    }


    public void setTimezone( String timezone )
    {
        this.timezone = timezone;
    }


    public Set<Token> getTokens()
    {
        return tokens;
    }


    @Override
    public ClassName getClassName()
    {
        return ClassName.UserAccount;
    }
}
