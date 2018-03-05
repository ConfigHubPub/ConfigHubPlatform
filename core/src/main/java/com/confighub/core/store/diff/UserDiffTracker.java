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
