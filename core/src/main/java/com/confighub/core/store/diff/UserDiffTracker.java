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

package com.confighub.core.store.diff;

import com.confighub.core.security.Token;
import com.confighub.core.store.APersisted;

import javax.persistence.*;

/**
 *
 */
public class UserDiffTracker
        extends ADiffTracker
{
    @PreRemove
    public void preRemove(APersisted obj)
    {
        obj.revType = APersisted.RevisionType.Delete;
        markForNotification();
    }

    @PrePersist
    public void prePersist(APersisted obj)
    {
        obj.revType = APersisted.RevisionType.Add;
    }

    @PostPersist
    public void postPersist(APersisted obj)
    {
        markForNotification();
    }


    @PostLoad
    public void loadOldData(APersisted obj)
    {
        if (!isTracked() || !(obj instanceof Token))
            return;

    }

    @PreUpdate
    public void preUpdate(APersisted obj)
    {

    }
}
