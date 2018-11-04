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
import com.confighub.core.store.APersisted;
import com.confighub.core.utils.Validator;

import javax.persistence.*;


@Entity
@Table( name = "email" )
public class Email
      extends APersisted
{
    @Id
    @GeneratedValue
    private Long id;

    @Column( nullable = false,
             unique = true )
    private String email;

    @OneToOne( fetch = FetchType.LAZY,
               cascade = { CascadeType.REFRESH } )
    private UserAccount user;


    protected Email()
    {
    }


    public Email( String email,
                  UserAccount user )
    {
        this.email = email;
        this.user = user;
    }

    // --------------------------------------------------------------------------------------------
    // Setters and getters
    // --------------------------------------------------------------------------------------------


    @PostUpdate
    @PostPersist
    public void enforce()
          throws ConfigException
    {
        if ( !Validator.validEmail( email ) )
        {
            throw new ConfigException( Error.Code.USER_BAD_EMAIL );
        }
    }


    @Override
    public Long getId()
    {
        return id;
    }


    public String getEmail()
    {
        return email;
    }


    public void setEmail( String email )
    {
        if ( this.email.equals( email ) )
        {
            return;
        }

        this.email = email;
    }


    public UserAccount getUser()
    {
        return user;
    }


    public void setUser( UserAccount user )
    {
        this.user = user;
    }


    @Override
    public ClassName getClassName()
    {
        return ClassName.Email;
    }
}
