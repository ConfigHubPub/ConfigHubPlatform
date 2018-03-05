/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.core.store;

import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Container for additional information relevant while auditing any entry.
 * This will make searching for cloud specific revision faster, and tracks
 * change author.
 */
public class RevisionEntityContext
{
    private static final Logger log = LogManager.getLogger(RevisionEntityContext.class);

    private Long userId;
    private String appId;
    private long repositoryId;
    private String changeComment;
    private boolean notify;
    private Set<String> searchKey = new HashSet<>();

    // Special handle for context re-sizing.  This may affect a very broad range of objects,
    // and therefore needs to be controlled in a special way.
    private boolean contextResize;

    private final Set<APersisted.ClassName> type = new HashSet<>();
    private final Map<Long, APersisted.RevisionType> revTypes = new HashMap<>();

    public void setAPersisted(final APersisted obj)
    {
        if (null == obj || null == obj.revType)
        {
            log.error("Trying to save object without class definition.  " + obj);
            System.out.println("Trying to save object without class definition.  " + obj);
            throw new ConfigException(Error.Code.AUDITED_NO_CLASS_DEF);
        }

        this.type.add(obj.getClassName());
        this.revTypes.put(obj.getId(), obj.revType);
    }

    public Map<Long, APersisted.RevisionType> getRevTypes()
    {
        return revTypes;
    }

    public Set<APersisted.ClassName> getType()
    {
        return type;
    }

    public void setAppId(String appId)
    {
        this.appId = appId;
    }

    public String getAppId()
    {
        return appId;
    }

    public String getChangeComment()
    {
        return changeComment;
    }

    public void setChangeComment(String changeComment)
    {
        this.changeComment = changeComment;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(long userId)
    {
        this.userId = userId;
    }

    public void setRepositoryId(long repositoryId)
    {
        this.repositoryId = repositoryId;
    }

    public long getRepositoryId()
    {
        return repositoryId;
    }

    public boolean isNotify()
    {
        return notify;
    }

    public void setNotify(boolean notify)
    {
        this.notify = notify;
    }

    public Set<String> getSearchKey()
    {
        return searchKey;
    }

    public void setSearchKey(String searchKey)
    {
        this.searchKey.add(searchKey);
    }

    public boolean isContextResize()
    {
        return contextResize;
    }

    public void setContextResize(boolean contextResize)
    {
        this.contextResize = contextResize;
    }
}
