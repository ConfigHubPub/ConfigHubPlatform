/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.core.store;

import com.confighub.core.repository.RepoFile;
import com.confighub.core.user.UserAccount;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

@Audited
@MappedSuperclass
public abstract class APersisted
{
    private static final Logger log = LogManager.getLogger(APersisted.class);

    public enum ClassName
    {
        UserAccount (UserAccount.class),
        Account (com.confighub.core.user.Account.class),
        Repository(com.confighub.core.repository.Repository.class),
        Organization(com.confighub.core.organization.Organization.class),
        ContextItem(com.confighub.core.repository.Level.class),
        SecurityProfile(com.confighub.core.security.SecurityProfile.class),
        Token(com.confighub.core.security.Token.class),
        Property(com.confighub.core.repository.Property.class),
        PropertyKey(com.confighub.core.repository.PropertyKey.class),
        Team(com.confighub.core.organization.Team.class),
        AccessRule(com.confighub.core.rules.AccessRule.class),
        Email(com.confighub.core.user.Email.class),
        Tag(com.confighub.core.repository.Tag.class),
        RepoFile(RepoFile.class),
        AbsoluteFilePath(com.confighub.core.repository.AbsoluteFilePath.class);


        Class clazz;
        ClassName(Class clazz)
        {
            this.clazz = clazz;
        }

        public Class getClazz() {
            return this.clazz;
        }
    }

    public enum RevisionType {
        Add, Modify, Delete
    }

    public transient RevisionType revType;
    public abstract Long getId();
    public abstract ClassName getClassName();

    @Column(name = "diffJson", length = 10485760, columnDefinition = "TEXT")
    public String diffJson;

    public String getDiffJson()
    {
        return diffJson;
    }

}
