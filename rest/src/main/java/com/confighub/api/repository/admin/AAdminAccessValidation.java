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

package com.confighub.api.repository.admin;

import com.confighub.api.server.auth.TokenState;
import com.confighub.core.error.ConfigException;
import com.confighub.core.repository.Repository;
import com.confighub.core.store.Store;
import com.confighub.core.store.diff.ADiffTracker;
import com.confighub.core.user.UserAccount;
import com.confighub.core.utils.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.core.Response;


public abstract class AAdminAccessValidation
{
    private static final Logger log = LogManager.getLogger( AAdminAccessValidation.class );

    protected UserAccount user;

    protected Repository repository;


    protected int validate( final String account,
                            final String repositoryName,
                            final String token,
                            final Store store )
    {
        return validate( account, repositoryName, token, store, false );
    }


    protected int validateWrite( final String account,
                                 final String repositoryName,
                                 final String token,
                                 final Store store,
                                 final boolean enableTracking )
    {
        if ( enableTracking )
        {
            ADiffTracker.track();
        }

        int status = validate( account, repositoryName, token, store, enableTracking );

        if ( 0 != status )
        {
            return status;
        }

        if ( null == this.user )
        {
            // Not authorized - 401
            return Response.Status.UNAUTHORIZED.getStatusCode();
        }

        if ( !this.repository.hasWriteAccess( this.user ) || !this.repository.isAdminOrOwner( this.user ) )
        {
            // Forbidden - 403
            return Response.Status.FORBIDDEN.getStatusCode();
        }

        return 0;
    }


    protected int validate( final String account,
                            final String repositoryName,
                            final String token,
                            final Store store,
                            final boolean enableTracking )
    {
        if ( Utils.anyBlank( account, repositoryName ) )
        {
            // Bad request - 400
            return Response.Status.BAD_REQUEST.getStatusCode();
        }

        try
        {
            this.repository = store.getRepository( account, repositoryName );
            if ( null == this.repository )
            {
                return Response.Status.NOT_FOUND.getStatusCode();
            }

            this.user = TokenState.getUser( token, store );

            if ( !repository.isDemo() )
            {

                if ( null == this.user )
                {
                    // Not authorized - 401
                    return Response.Status.UNAUTHORIZED.getStatusCode();
                }

                if ( !this.repository.isAdminOrOwner( this.user ) )
                {
                    // Forbidden - 403
                    return Response.Status.FORBIDDEN.getStatusCode();
                }
            }
        }
        catch ( final ConfigException e )
        {
            log.error( e.getMessage() );
            e.printStackTrace();

            return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        }

        return 0;
    }
}