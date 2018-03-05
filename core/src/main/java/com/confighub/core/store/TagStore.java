/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.core.store;

import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.repository.Repository;
import com.confighub.core.repository.Tag;
import com.confighub.core.user.UserAccount;
import com.confighub.core.utils.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 */
public class TagStore
        extends Store
{
    private static final Logger log = LogManager.getLogger(TagStore.class);


    public void createTag(final UserAccount user,
                          final Repository repository,
                          final String name,
                          final String readme,
                          final Long ts)
            throws ConfigException
    {
        if (Utils.anyNull(repository, name, ts))
            throw new ConfigException(Error.Code.MISSING_PARAMS);

        if (!repository.hasWriteAccess(user))
            throw new ConfigException(Error.Code.USER_ACCESS_DENIED);

        Tag tag = new Tag(repository);
        tag.setName(name);
        tag.setReadme(readme);

        if (ts < 0 || ts > System.currentTimeMillis())
            tag.setTs(System.currentTimeMillis());
        else
            tag.setTs(ts);

        saveOrUpdateAudited(user, repository, tag);
    }

    public void deleteTag(final UserAccount user, final Repository repository, final String name)
            throws ConfigException
    {
        if (Utils.anyNull(repository, name))
            throw new ConfigException(Error.Code.MISSING_PARAMS);

        if (!repository.hasWriteAccess(user))
            throw new ConfigException(Error.Code.USER_ACCESS_DENIED);

        Tag tag = getTag(repository.getId(), name);
        if (null == tag)
            return;

        deleteAudited(user, repository, tag);
    }


    public void editTag(final UserAccount user,
                        final Repository repository,
                        final String oldName,
                        final String name,
                        final String readme,
                        final Long ts)
            throws ConfigException
    {
        if (Utils.anyNull(repository, oldName, name, ts))
            throw new ConfigException(Error.Code.MISSING_PARAMS);

        if (!repository.hasWriteAccess(user))
            throw new ConfigException(Error.Code.USER_ACCESS_DENIED);

        Tag tag = getTag(repository.getId(), oldName);
        if (null == tag)
            throw new ConfigException(Error.Code.NOT_FOUND);

        tag.setName(name);
        tag.setReadme(readme);
        tag.setTs(ts);

        saveOrUpdateAudited(user, repository, tag);
    }


}
