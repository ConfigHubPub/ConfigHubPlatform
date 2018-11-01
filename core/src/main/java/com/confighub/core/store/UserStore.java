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

import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.organization.Team;
import com.confighub.core.repository.Repository;
import com.confighub.core.user.UserAccount;
import com.confighub.core.utils.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UserStore
        extends Store
{
    private static final Logger log = LogManager.getLogger(UserStore.class);

    /**
     * Confirm email validation.
     *
     * @param user to be updated
     * @return user that was updated
     * @throws ConfigException
     */
    public UserAccount confirmEmail(final UserAccount user)
            throws ConfigException
    {
        if (null == user)
            throw new ConfigException(Error.Code.MISSING_PARAMS);

//        user.setEmailVerified(true);
        saveOrUpdateNonAudited(user);
        return user;
    }


    /**
     *
     * @param user
     * @param oldPassword
     * @param newPassword
     * @return
     * @throws ConfigException
     */
    public UserAccount changePassword(final UserAccount user, final String oldPassword, final String newPassword)
            throws ConfigException
    {
        if (Utils.anyBlank(oldPassword, newPassword))
            throw new ConfigException(Error.Code.MISSING_PARAMS);

        if (Utils.anyNull(user))
            throw new ConfigException(Error.Code.MISSING_PARAMS);

        if (!user.isPasswordValid(oldPassword))
            throw new ConfigException(Error.Code.USER_AUTH);

        user.setUserPassword( newPassword);
        saveOrUpdateNonAudited(user);

        return user;
    }

    /**
     *
     * @param user
     * @param email
     * @return
     * @throws ConfigException
     */
    public UserAccount changeEmail(final UserAccount user, final String email)
            throws ConfigException
    {
        if (Utils.anyNull(user))
            throw new ConfigException(Error.Code.MISSING_PARAMS);

        user.setEmail(email);

        saveOrUpdateNonAudited(user);
        return user;
    }

    /**
     *
     * @param user
     * @param val
     * @throws ConfigException
     */
    public void emailRepoCritical(final UserAccount user, boolean val)
        throws ConfigException
    {
        if (Utils.anyNull(user))
            throw new ConfigException(Error.Code.MISSING_PARAMS);

        user.setEmailRepoCritical(val);
        saveOrUpdateNonAudited(user);
    }

    /**
     *
     * @param user
     * @param val
     * @throws ConfigException
     */
    public void emailBlog(final UserAccount user, boolean val)
            throws ConfigException
    {
        if (Utils.anyNull(user))
            throw new ConfigException(Error.Code.MISSING_PARAMS);

        user.setEmailBlog(val);
        saveOrUpdateNonAudited(user);
    }

    /**
     *
     * @param user
     * @param repositoryAccountName
     * @param repositoryName
     * @return
     * @throws ConfigException
     */
    public boolean leaveRepository(final UserAccount user, String repositoryAccountName, String repositoryName)
        throws ConfigException
    {
        if (Utils.anyNull(user, repositoryAccountName, repositoryName))
            throw new ConfigException(Error.Code.MISSING_PARAMS);

        Repository repository = getRepository(repositoryAccountName, repositoryName);
        if (null == repository)
            return false;

        Team team = removeTeamMember(repository, user, user);
        user.getAccount().removeRepository(repository);

        saveOrUpdateAudited(user, repository, team);
        saveOrUpdateNonAudited(user);

        return false;
    }
}
