/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
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

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@NamedQueries({
        @NamedQuery(name = "User.count", query = "SELECT COUNT(c) FROM UserAccount c WHERE c.active=true"),
        @NamedQuery(name = "User.loginByUsername", query = "SELECT u FROM UserAccount u WHERE u.account.name=:username"),
        @NamedQuery(name = "User.loginByEmail", query = "SELECT u FROM UserAccount u WHERE email.email=:email "),
        @NamedQuery(name = "User.isRegistered", query = "SELECT COUNT(u.email.email) FROM UserAccount u WHERE email.email=:email"),
        @NamedQuery(name = "User.isUsernameTaken", query = "SELECT COUNT(u.account.name) FROM UserAccount u WHERE account.name=:username"),
        @NamedQuery(name = "User.getByUsername", query = "SELECT u FROM UserAccount u WHERE u.account.name=:username"),
        @NamedQuery(name = "Users.search", query = "SELECT u FROM UserAccount u WHERE u.account.name LIKE :searchTerm OR u.name LIKE :searchTerm")
})
@EntityListeners({ UserDiffTracker.class })
public class UserAccount
        extends APersisted
{
    private static final Logger log = LogManager.getLogger(UserAccount.class);

    @Id
    @GeneratedValue
    private Long id;

    @JoinColumn(nullable = false, unique = true)
    @OneToOne(fetch = FetchType.LAZY, cascade = { CascadeType.ALL })
    private Email email;

    @JoinColumn(nullable = false, unique= true)
    @OneToOne(fetch = FetchType.LAZY, cascade = { CascadeType.ALL })
    private Account account;

    @Column(nullable = false)
    private String password;

    @Column(name = "name")
    private String name;

    @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.REFRESH })
    private Set<Organization> organizations;

    @Column(nullable = false)
    private Date createDate;

    @Column(name = "timezone")
    private String timezone;

    @Column(name = "active")
    private boolean active;

    @Column(name = "configHubAdmin")
    private boolean configHubAdmin = false;

    // Notification preferences
    @Column(name = "emailRepoCritical")
    private boolean emailRepoCritical = true;

    @Column(name = "emailBlog")
    private boolean emailBlog = true;

    @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.REFRESH, CascadeType.PERSIST })
//    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @NotAudited
    private Set<Token> tokens;

    @Override
    public String toString()
    {
        return String.format("%d | %s | %s | [%s]",
                             this.id,
                             this.email.getEmail(),
                             this.name,
                             this.active ? "active" : "INACTIVE");
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
     * 2. password specified and satisfies requirements
     */
    @PreUpdate
    public void enforce()
            throws ConfigException
    {
        if (Utils.anyBlank(this.password))
            throw new ConfigException(Error.Code.USER_AUTH);

        // ToDo define and enforce password requirements
    }

    // -------------------------------------------------------------------------------------------- //
    // Organizations
    // -------------------------------------------------------------------------------------------- //

    public void joinOrganization(final Organization organization)
    {
        if (null == organization) return;

        if (null == this.organizations)
            this.organizations = new HashSet<>();

        this.organizations.add(organization);
    }

    public void removeOrganization(final Organization organization)
    {
        if (null == organization) return;
        if (null == this.organizations) return;

        this.organizations.remove(organization);
    }

    public int getOrganizationCount()
    {
        if (null == this.organizations)
            return 0;

        return this.organizations.size();
    }

    // -------------------------------------------------------------------------------------------- //
    // POJO Operations
    // -------------------------------------------------------------------------------------------- //

    @Override
    public boolean equals(Object other)
    {
        if (null == other)
            return false;

        if (!(other instanceof UserAccount))
            return false;

        UserAccount o = (UserAccount) other;
        return this.getEmail().equals(o.getEmail());
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

    public void setEmail(String email)
    {
        if (null == this.email)
            this.email = new Email(email, this);
        else
            this.email.setEmail(email);
    }

    public void setPassword(String password)
            throws ConfigException
    {
        if (!Utils.passwordRequirementsSatisfied(password))
            throw new ConfigException(Error.Code.PASSWORD_REQUIREMENTS);
        this.password = Passwords.generateStrongPasswordHash(password);
    }

    public boolean isPasswordValid(String challenge)
            throws ConfigException
    {
        return Passwords.validatePassword(challenge, this.password);
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Set<Repository> getRepositories()
    {
        return this.account.getRepositories();
    }

    public int getRepositoryCount() {
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

    public void setUsername(String username)
    {
        if (null == this.account)
        {
            this.account = new Account();
            this.account.setUser(this);
        }

        this.account.setName(username);
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

    public void setActive(boolean active)
    {
        this.active = active;
    }

    public boolean isConfigHubAdmin()
    {
        return configHubAdmin;
    }

    public boolean isEmailRepoCritical()
    {
        return emailRepoCritical;
    }

    public void setEmailRepoCritical(boolean emailRepoCritical)
    {
        this.emailRepoCritical = emailRepoCritical;
    }

    public boolean isEmailBlog()
    {
        return emailBlog;
    }

    public void setEmailBlog(boolean emailBlog)
    {
        this.emailBlog = emailBlog;
    }

    public String getTimezone()
    {
        return timezone;
    }

    public void setTimezone(String timezone)
    {
        this.timezone = timezone;
    }

    public Set<Token> getTokens()
    {
        return tokens;
    }

    @Override
    public ClassName getClassName() {
        return ClassName.UserAccount;
    }
}
