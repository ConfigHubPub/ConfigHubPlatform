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

import com.confighub.core.security.CipherTransformation;
import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.repository.Repository;
import com.confighub.core.security.SecurityProfile;
import com.confighub.core.user.UserAccount;
import com.confighub.core.utils.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.NoResultException;
import java.util.Collection;

public class SecurityStore
        extends Store
{
    private static final Logger log = LogManager.getLogger(SecurityStore.class);

    /**
     * Create a EncryptionProfile.  Author must be a repository owner or admin.
     *
     * @param author
     * @param repository
     * @param name
     * @param secret
     * @return
     * @throws ConfigException
     */
    public SecurityProfile create(final UserAccount author,
                                    final Repository repository,
                                    final CipherTransformation cipher,
                                    final String name,
                                    final String secret)
            throws ConfigException
    {
        if (Utils.anyNull(author, repository, name, secret))
            throw new ConfigException(Error.Code.MISSING_PARAMS);

        if (!repository.isAdminOrOwner(author))
            throw new ConfigException(Error.Code.USER_ACCESS_DENIED);

        SecurityProfile group = new SecurityProfile(repository, cipher, name, secret);
        saveOrUpdateAudited(author, repository, group);

        return group;
    }



    public Collection<SecurityProfile> getAllRepoEncryptionProfiles(UserAccount admin, Repository repository)
            throws ConfigException
    {
        if (null == repository)
            throw new ConfigException(Error.Code.MISSING_PARAMS);

        if (!repository.hasReadAccess(admin))
            throw new ConfigException(Error.Code.USER_ACCESS_DENIED);

        try
        {
            return em.createNamedQuery("SecurityProfile.getAll")
                     .setParameter("repository", repository)
                     .getResultList();
        }
        catch (NoResultException e)
        {
            return null;
        }
        catch (Exception e)
        {
            handleException(e);
        }

        return null;
    }

}
