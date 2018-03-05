/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.core.organization;

import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.repository.Repository;
import com.confighub.core.store.APersisted;
import com.confighub.core.user.Account;
import com.confighub.core.user.UserAccount;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Organization
        extends APersisted
{
    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "name")
    private String name;

    @OneToOne(fetch = FetchType.LAZY, cascade = { CascadeType.ALL })
    private Account account;

    @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.REFRESH })
    @Column(nullable = false)
    @JoinTable(name="Organization_Owners")
    private Set<UserAccount> owners;

    @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.REFRESH })
    @JoinTable(name="Organization_Admins")
    private Set<UserAccount> administrators;

    @Column(nullable = false)
    private Date createDate;

    // --------------------------------------------------------------------------------------------
    // User management
    // --------------------------------------------------------------------------------------------

    public void addOwner(UserAccount owner)
        throws ConfigException
    {
        if (null == owner)
            throw new ConfigException(Error.Code.MISSING_PARAMS);

        if (isOwnerOrAdmin(owner))
            throw new ConfigException(Error.Code.ORG_EXISTING_MANAGER);

        if (null == this.owners)
            this.owners = new HashSet<>();

        this.owners.add(owner);
        owner.joinOrganization(this);
    }

    public boolean removeOwner(UserAccount owner)
        throws ConfigException
    {
        if (null == this.owners) return false;
        if (null == owner) return false;

        if (this.owners.size() == 1)
            throw new ConfigException(Error.Code.ORG_NO_OWNERS);

        owner.removeOrganization(this);
        return this.owners.remove(owner);
    }

    public int getOwnerCount()
    {
        return this.owners.size();
    }

    public void addAdministrator(UserAccount admin)
        throws ConfigException
    {
        if (null == admin)
            throw new ConfigException(Error.Code.MISSING_PARAMS);

        if (isOwnerOrAdmin(admin))
            throw new ConfigException(Error.Code.ORG_EXISTING_MANAGER);

        if (null == this.administrators)
            this.administrators = new HashSet<>();

        this.administrators.add(admin);
        admin.joinOrganization(this);
    }

    public boolean removeAdministrator(UserAccount admin)
    {
        if (null == this.administrators) return false;
        if (null == admin) return false;

        admin.removeOrganization(this);
        return this.administrators.remove(admin);
    }

    public int getAdminCount()
    {
        if (null == this.administrators) return 0;
        return this.administrators.size();
    }


    public boolean isOwnerOrAdmin(UserAccount user)
    {
        if (null != this.owners && this.owners.contains(user))
            return true;

        return null != this.administrators && this.administrators.contains(user);
    }

    public boolean isOwner(UserAccount user)
    {
        return this.owners.contains(user);
    }

    public boolean isAdmin(UserAccount user)
    {
        return null != this.administrators && this.administrators.contains(user);
    }


    // --------------------------------------------------------------------------------------------
    // Validation
    // --------------------------------------------------------------------------------------------

    @PrePersist
    protected void setCreationDate() {
        this.createDate = new Date();
    }

    @PreRemove
    public void removeFromUsers()
    {
        for (UserAccount owner : this.owners)
            owner.removeOrganization(this);
        this.owners.clear();

        if (null != this.administrators)
        {
            for (UserAccount admin : this.administrators)
                admin.removeOrganization(this);

            this.administrators.clear();
        }
    }

    // --------------------------------------------------------------------------------------------
    // POJO Ops
    // --------------------------------------------------------------------------------------------

    @Override
    public boolean equals(Object o)
    {
        if (null == o)
            return false;

        return o instanceof Organization && ((Organization)o).getId().equals(this.getId());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.account);
    }

    // --------------------------------------------------------------------------------------------
    // Setters and getters
    // --------------------------------------------------------------------------------------------

    @Override
    public Long getId()
    {
        return this.id;
    }

    public String getAccountName()
    {
        return this.account.getName();
    }

    public void setAccountName(String name)
    {
        if (null == this.account)
        {
            this.account = new Account();
            this.account.setOrganization(this);
        }
        this.account.setName(name);
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Account getAccount()
    {
        return this.account;
    }

    public Set<UserAccount> getOwners()
    {
        return owners;
    }

    public Set<UserAccount> getAdministrators()
    {
        return administrators;
    }

    public Date getCreateDate()
    {
        return createDate;
    }

    public Set<Repository> getRepositories()
    {
        return this.account.getRepositories();
    }

    public int getRepositoryCount() {
        return this.account.getRepositoryCount();
    }

    @Override
    public ClassName getClassName() {
        return ClassName.Organization;
    }

}
