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
import com.confighub.core.repository.Repository;
import com.confighub.core.user.UserAccount;
import com.confighub.core.utils.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.exception.ConstraintViolationException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.OptimisticLockException;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;


public abstract class AStore
{
    private static final Logger log = LogManager.getLogger( AStore.class );

    public static boolean verbose = false;

    /**
     * <url>http://stackoverflow.com/questions/4543947/when-should-entitymanagerfactory-instance-be-created-opened</url>
     * <p>
     * - EntityManagerFactory instances are heavyweight objects. Each factory might maintain a metadata cache,
     * object state cache, EntityManager pool, connection pool, and more. If your application no longer needs an
     * EntityManagerFactory, you should close it to free these resources.
     * <p>
     * - When an EntityManagerFactory closes, all EntityManagers from that factory, and by extension all entities
     * managed by those EntityManagers, become invalid.
     * <p>
     * - It is much better to keep a factory open for a long period of time than to repeatedly create and close new
     * factories. Thus, most applications will never close the factory, or only close it when the application is
     * exiting.
     * <p>
     * - Only applications that require multiple factories with different configurations have an obvious reason to
     * create and close multiple EntityManagerFactory instances.
     * <p>
     * - Only one EntityManagerFactory is permitted to be created for each deployed persistence unit configuration.
     * Any number of EntityManager instances may be created from a given factory.
     * <p>
     * - More than one entity manager factory instance may be available simultaneously in the JVM. Methods of the
     * EntityManagerFactory interface are thread-safe.
     */
    private static final EntityManagerFactory emf;

    protected EntityManager em;

    static
    {
         emf = Persistence.createEntityManagerFactory( "ConfigHubMain" );
    }

    public AStore()
    {
        this.em = emf.createEntityManager();
    }


    public void begin()
    {
        em.getTransaction().begin();
    }


    protected boolean saveOrUpdateNonAudited( Object toSave )
          throws ConfigException
    {
        if ( null == toSave )
        {
            return false;
        }

        try
        {
            em.persist( toSave );
            em.flush();
        }
        catch ( ConfigException e )
        {
            throw e;
        }
        catch ( Exception e )
        {
            handleException( e );
        }

        return true;
    }


    protected boolean delete( APersisted toDelete )
    {
        if ( null == toDelete )
        {
            return false;
        }

        try
        {
            em.remove( em.contains( toDelete ) ? toDelete : em.merge( toDelete ) );
            em.flush();
        }
        catch ( ConfigException e )
        {
            throw e;
        }
        catch ( EntityNotFoundException ignore )
        {
        }
        catch ( Exception e )
        {
            handleException( e );
        }
        return true;
    }


    protected boolean deleteAudited( final UserAccount user,
                                     final Repository repository,
                                     APersisted toDelete )
          throws ConfigException
    {
        return deleteAudited( user, repository, toDelete, null );
    }


    protected boolean deleteAudited( final UserAccount user,
                                     final Repository repository,
                                     APersisted toDelete,
                                     String changeComment )
          throws ConfigException
    {
        if ( null == toDelete )
        {
            return false;
        }

        try
        {
            RevisionEntityContext revContext = ThreadLocalRevEntry.get();
            if ( null == revContext )
            {
                revContext = new RevisionEntityContext();
            }

            if ( null != repository )
            {
                revContext.setRepositoryId( repository.getId() );
            }

            toDelete.revType = APersisted.RevisionType.Delete;
            revContext.setAPersisted( toDelete );
            revContext.setUserId( user.getId() );
            revContext.setChangeComment( changeComment );

            ThreadLocalRevEntry.set( revContext );

            em.remove( toDelete );
            em.flush();
        }
        catch ( EntityNotFoundException ignore )
        {
            ignore.printStackTrace();
        }
        catch ( ConfigException e )
        {
            throw e;
        }
        catch ( Exception e )
        {
            handleException( e );
        }

        return true;
    }


    protected boolean deleteAuditedViaAPI( final String appIdentifier,
                                           final Repository repository,
                                           APersisted toDelete,
                                           String changeComment )
          throws ConfigException
    {
        if ( null == toDelete )
        {
            return false;
        }

        try
        {
            RevisionEntityContext revContext = ThreadLocalRevEntry.get();
            if ( null == revContext )
            {
                revContext = new RevisionEntityContext();
            }

            if ( null != repository )
            {
                revContext.setRepositoryId( repository.getId() );
            }

            toDelete.revType = APersisted.RevisionType.Delete;
            revContext.setAPersisted( toDelete );

            if ( !Utils.isBlank( appIdentifier ) )
            {
                revContext.setAppId( appIdentifier );
            }

            revContext.setChangeComment( changeComment );

            ThreadLocalRevEntry.set( revContext );

            em.remove( toDelete );
            em.flush();
        }
        catch ( EntityNotFoundException ignore )
        {
            ignore.printStackTrace();
        }
        catch ( ConfigException e )
        {
            throw e;
        }
        catch ( Exception e )
        {
            handleException( e );
        }

        return true;
    }


    protected boolean saveOrUpdateAudited( final UserAccount user,
                                           final Repository repository,
                                           final APersisted toSave )
          throws ConfigException
    {
        return saveOrUpdateAudited( user, repository, toSave, null );
    }


    protected boolean saveOrUpdateAuditedViaAPI( final String appIdentifier,
                                                 final Repository repository,
                                                 final APersisted toSave,
                                                 final String changeComment )
          throws ConfigException
    {
        if ( null == toSave )
        {
            return false;
        }

        try
        {
            em.persist( toSave );
            em.flush();

            RevisionEntityContext revContext = ThreadLocalRevEntry.get();
            if ( null == revContext )
            {
                revContext = new RevisionEntityContext();
            }

            if ( null != repository )
            {
                revContext.setRepositoryId( repository.getId() );
            }

            if ( !Utils.isBlank( appIdentifier ) )
            {
                revContext.setAppId( appIdentifier );
            }

            revContext.setChangeComment( changeComment );

            revContext.setAPersisted( toSave );
            ThreadLocalRevEntry.set( revContext );
        }
        catch ( ConfigException e )
        {
            throw e;
        }
        catch ( Exception e )
        {
            handleException( e );
        }

        return true;
    }


    protected boolean saveOrUpdateAudited( final UserAccount user,
                                           final Repository repository,
                                           final APersisted toSave,
                                           final String changeComment )
          throws ConfigException
    {
        if ( null == toSave )
        {
            return false;
        }

        if ( null == user )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        try
        {
            em.persist( toSave );
            em.flush();

            RevisionEntityContext revContext = ThreadLocalRevEntry.get();
            if ( null == revContext )
            {
                revContext = new RevisionEntityContext();
            }

            if ( null != repository )
            {
                revContext.setRepositoryId( repository.getId() );
            }

            revContext.setUserId( user.getId() );
            revContext.setChangeComment( changeComment );

            revContext.setAPersisted( toSave );
            ThreadLocalRevEntry.set( revContext );
        }
        catch ( ConfigException e )
        {
            throw e;
        }
        catch ( Exception e )
        {
            handleException( e );
        }

        return true;
    }


    public void commit()
          throws ConfigException
    {
        try
        {
            em.getTransaction().commit();
        }
        catch ( ConfigException e )
        {
            throw e;
        }
        catch ( Exception e )
        {
            handleException( e );
        }
    }


    // ToDo - go over visibility of this method due to the security concerns
    public <T> T get( Class clazz,
                      Long id )
    {
        if ( null == id )
        {
            return null;
        }

        return (T) em.find( clazz, id );
    }


    public UserAccount getUser( final Long id )
    {
        if ( null == id )
        {
            return null;
        }
        return get( UserAccount.class, id );
    }


    public <T> T merge( T o )
    {
        return em.merge( o );
    }


    public void rollback()
    {
        if ( em.isOpen() )
        {
            EntityTransaction transaction = em.getTransaction();
            if ( transaction.isActive() )
            {
                transaction.rollback();
            }
        }
    }


    public void close()
    {
        EntityTransaction transaction = em.getTransaction();

        if ( transaction.isActive() )
        {
            transaction.rollback();
        }
        em.close();
    }


    protected static void handleException( Exception e )
          throws ConfigException
    {
        if ( verbose )
        {
            e.printStackTrace();
            log.error( e.getMessage() );
        }

        Throwable t = e;
        Error.Code code = Error.Code.CATCH_ALL;
        do
        {
            if ( t instanceof OptimisticLockException )
            {
                code = Error.Code.DB_LOCKING;
            }
            else if ( t instanceof javax.persistence.RollbackException )
            {
                code = Error.Code.INTERNAL_ERROR;
            }
            else if ( t instanceof org.hibernate.exception.LockAcquisitionException ||
//                      t instanceof com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException ||
                      t instanceof ConstraintViolationException ||
                      t instanceof PersistenceException )
            {
                code = Error.Code.CONSTRAINT;
            }
            else if ( t instanceof NoResultException )
            {
                throw new ConfigException( Error.Code.NOT_FOUND );
            }
            else if ( t instanceof ConfigException )
            {
                throw (ConfigException) t;
            }

            t = t.getCause();
        }
        while ( t != null );

        if ( Error.Code.CATCH_ALL.equals( code ) )
        {
            // ToDo: this exception should be decorated with instructions to go to system error page
            System.out.println( "----------------- Un-handled exception start --------------------" );
            e.printStackTrace();
            System.out.println( "----------------- Un-handled exception end   --------------------" );
            log.error( "----------------- Un-handled exception start --------------------" );
            log.error( e.getMessage() );
            log.error( "----------------- Un-handled exception end   --------------------" );
        }
        throw new ConfigException( code );
    }



    // --------------------------------------------------------------------------------------------
    // Existence checks
    // --------------------------------------------------------------------------------------------
    public boolean isAccountNameUsed( String name )
    {
        try
        {
            long count = (long) em.createNamedQuery( "AccountName.count" ).setParameter( "name", name ).getSingleResult();
            return count > 0;
        }
        catch ( NoResultException e )
        {
            return false;
        }
    }
}
