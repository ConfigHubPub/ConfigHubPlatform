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