/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.core.user;

import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.store.APersisted;
import com.confighub.core.utils.Validator;

import javax.persistence.*;

@Entity
public class Email
        extends APersisted
{
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @OneToOne(fetch = FetchType.LAZY, cascade = { CascadeType.REFRESH })
    private UserAccount user;

    protected Email() {}

    public Email(String email, UserAccount user)
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
        if (!Validator.validEmail(email))
            throw new ConfigException(Error.Code.USER_BAD_EMAIL);
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

    public void setEmail(String email)
    {
        if (this.email.equals(email))
            return;

        this.email = email;
    }

    public UserAccount getUser()
    {
        return user;
    }

    public void setUser(UserAccount user)
    {
        this.user = user;
    }

    @Override
    public ClassName getClassName() {
        return ClassName.Email;
    }
}
