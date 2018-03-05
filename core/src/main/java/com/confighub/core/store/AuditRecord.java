/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.core.store;

import com.confighub.core.user.UserAccount;
import com.confighub.core.utils.DateTimeUtils;

import java.util.Date;
import java.util.List;

/**
 * When tracking history of an Audited item, each historical entry is wrapped
 * in AuditRecord, thus providing additional information for each audit instance.
 */
public class AuditRecord
{
    private List<APersisted> audited;
    private final RevisionEntry revEntry;
    private final UserAccount author;
    private final String appId;

    protected AuditRecord(final List<APersisted> audited,
                          final RevisionEntry revEntry,
                          final UserAccount author,
                          final String appId)
    {
        this.audited = audited;
        this.revEntry = revEntry;
        this.author = author;
        this.appId = appId;
    }

    @Override
    public String toString()
    {
        return String.format("%s:: rev: %d",
                             DateTimeUtils.standardDTFormatter.get().format(new Date(revEntry.getTimestamp())),
                             revEntry.getId());
    }

    /**
     * Entity at the point in time
     * @return APersisted Entity
     */
    public List<APersisted> getAudited()
    {
        return audited;
    }

    /**
     * Revision record
     * @return RevisionEntry
     */
    public RevisionEntry getRevEntry()
    {
        return revEntry;
    }

    /**
     * Change author
     * @return User
     */
    public UserAccount getAuthor()
    {
        return author;
    }

    public String getAppId()
    {
        return appId;
    }
}