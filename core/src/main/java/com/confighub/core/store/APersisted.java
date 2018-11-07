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

package com.confighub.core.store;

import com.confighub.core.repository.CtxLevel;
import com.confighub.core.repository.RepoFile;
import com.confighub.core.user.UserAccount;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;


@Audited
@MappedSuperclass
public abstract class APersisted
{
    private static final Logger log = LogManager.getLogger( APersisted.class );

    public enum ClassName
    {
        UserAccount( UserAccount.class ),
        Account( com.confighub.core.user.Account.class ),
        Repository( com.confighub.core.repository.Repository.class ),
        Organization( com.confighub.core.organization.Organization.class ),
        ContextItem( CtxLevel.class ),
        SecurityProfile( com.confighub.core.security.SecurityProfile.class ),
        Token( com.confighub.core.security.Token.class ),
        Property( com.confighub.core.repository.Property.class ),
        PropertyKey( com.confighub.core.repository.PropertyKey.class ),
        Team( com.confighub.core.organization.Team.class ),
        AccessRule( com.confighub.core.rules.AccessRule.class ),
        Email( com.confighub.core.user.Email.class ),
        Tag( com.confighub.core.repository.Tag.class ),
        RepoFile( RepoFile.class ),
        AbsoluteFilePath( com.confighub.core.repository.AbsoluteFilePath.class ),
        Configuration( com.confighub.core.system.SystemConfig.class );

        Class clazz;


        ClassName( Class clazz )
        {
            this.clazz = clazz;
        }


        public Class getClazz()
        {
            return this.clazz;
        }
    }

    public enum RevisionType
    {
        Add, Modify, Delete
    }

    public transient RevisionType revType;


    public abstract Long getId();

    public abstract ClassName getClassName();


    @Lob
    @Type( type = "org.hibernate.type.TextType" )
    @Column( name = "diffJson",
             columnDefinition = "TEXT" )
    public String diffJson;


    public String getDiffJson()
    {
        return diffJson;
    }
}
