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

import com.confighub.core.auth.Auth;
import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.organization.Organization;
import com.confighub.core.organization.Team;
import com.confighub.core.repository.*;
import com.confighub.core.rules.AccessRule;
import com.confighub.core.rules.AccessRuleWrapper;
import com.confighub.core.security.CipherTransformation;
import com.confighub.core.security.SecurityProfile;
import com.confighub.core.security.Token;
import com.confighub.core.system.SystemConfig;
import com.confighub.core.system.conf.LdapConfig;
import com.confighub.core.user.Account;
import com.confighub.core.user.UserAccount;
import com.confighub.core.utils.FileUtils;
import com.confighub.core.utils.Pair;
import com.confighub.core.utils.Utils;
import com.confighub.core.utils.Validator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;

import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.*;

import static com.confighub.core.store.Store.KeyUpdateStatus.UPDATE;


public class Store
      extends AStore
{
    private static final Logger log = LogManager.getLogger( Store.class );

    // --------------------------------------------------------------------------------------------
    // User
    // --------------------------------------------------------------------------------------------


    public List<UserAccount> searchUsers( final String searchTerm,
                                          int max )
    {
        try
        {
            return em.createNamedQuery( "Users.search" )
                     .setLockMode( LockModeType.NONE )
                     .setParameter( "searchTerm", String.format( "%%%s%%", searchTerm ) )
                     .setMaxResults( max <= 0 ? 10 : ( max > 100 ? 100 : max ) )
                     .getResultList();
        }
        catch ( NoResultException e )
        {
            return null;
        }
    }


    public UserAccount geUserByEmail( final String email )
    {
        if ( Utils.isBlank( email ) || !isEmailRegistered( email ) )
        {
            throw new ConfigException( Error.Code.ACCOUNT_NOT_FOUND );
        }

        UserAccount user = (UserAccount) em.createNamedQuery( "User.loginByEmail" )
                                           .setLockMode( LockModeType.NONE )
                                           .setParameter( "email", email )
                                           .getSingleResult();

        if ( null == user )
        {
            throw new ConfigException( Error.Code.ACCOUNT_NOT_FOUND );
        }

        return user;
    }


    public UserAccount getUserByUsername( final String username )
    {
        try
        {
            return (UserAccount) em.createNamedQuery( "User.getByUsername" )
                                   .setLockMode( LockModeType.NONE )
                                   .setParameter( "username", username )
                                   .getSingleResult();
        }
        catch ( NoResultException e )
        {
            return null;
        }
    }


    public Account getAccount( String name )
    {
        try
        {
            return (Account) em.createNamedQuery( "AccountName.get" )
                               .setLockMode( LockModeType.NONE )
                               .setParameter( "name", name )
                               .getSingleResult();
        }
        catch ( NoResultException e )
        {
            return null;
        }
    }


    public UserAccount updateUserPassword( final UserAccount user,
                                           final String password )
          throws ConfigException
    {
        if ( UserAccount.AccountType.LOCAL.equals( user.getAccountType() ) )
        {
            user.setUserPassword( password );
            saveOrUpdateNonAudited( user );
        }
        return user;
    }


    public UserAccount updateAccountName( final UserAccount user,
                                          final String accountName,
                                          final String newName,
                                          final String password )
          throws ConfigException
    {
        if ( Utils.anyNull( user, accountName, newName ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !user.isPasswordValid( password ) )
        {
            throw new ConfigException( Error.Code.USER_AUTH );
        }

        Account account = getAccount( accountName );
        if ( null == account )
        {
            throw new ConfigException( Error.Code.ACCOUNT_NOT_FOUND );
        }

        if ( account.isPersonal() )
        {
            if ( !account.equals( user.getAccount() ) )
            {
                throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
            }
        }
        else
        {
            Organization organization = account.getOrganization();
            if ( !organization.isOwner( user ) )
            {
                throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
            }
        }

        account.setName( newName );
        saveOrUpdateNonAudited( account );

        return user;
    }


    /**
     * @param user
     * @param name
     * @param company
     * @param website
     * @param city
     * @param country
     * @return
     * @throws ConfigException
     */
    public UserAccount updatePublicProfile( final UserAccount user,
                                            final String accountName,
                                            final String name,
                                            final String company,
                                            final String website,
                                            final String city,
                                            final String country )
          throws ConfigException
    {
        if ( Utils.anyNull( user, accountName ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        Account account = getAccount( accountName );
        if ( null == account )
        {
            throw new ConfigException( Error.Code.ACCOUNT_NOT_FOUND );
        }

        if ( account.isPersonal() )
        {
            if ( !account.equals( user.getAccount() ) )
            {
                throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
            }

            user.setName( name );
        }
        else
        {
            Organization organization = account.getOrganization();
            if ( !organization.isOwner( user ) )
            {
                throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
            }

            organization.setName( name );
        }

        account.setCompany( company );
        account.setWebsite( website );
        account.setCity( city );
        account.setCountry( country );

        saveOrUpdateNonAudited( user );
        return user;
    }


    /**
     * @param email
     * @param username
     * @param password
     * @return
     * @throws ConfigException
     */
    public UserAccount createUser( final String email,
                                   final String username,
                                   final String password,
                                   final UserAccount.AccountType accountType )
          throws ConfigException
    {
        if ( !Auth.isLocalAccountsEnabled() )
        {
            throw new ConfigException( Error.Code.LOCAL_ACCOUNTS_DISABLED );
        }

        if ( isEmailRegistered( email ) )
        {
            throw new ConfigException( Error.Code.EMAIL_REGISTERED );
        }

        UserAccount user = new UserAccount();
        user.setEmail( email );
        user.setUsername( username );
        user.setActive( true );
        user.setAccountType( accountType );
        user.setUserPassword( password );

        saveOrUpdateNonAudited( user );
        return user;
    }


    public UserAccount getUserAccount( final String username )
          throws ConfigException
    {
        if ( Utils.anyBlank( username ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        try
        {
            return (UserAccount) em.createNamedQuery( "User.loginByUsername" )
                                   .setLockMode( LockModeType.NONE )
                                   .setParameter( "username", username )
                                   .getSingleResult();
        }
        catch ( Exception e )
        {
            return null;
        }
    }


    /**
     * @param username of a user
     * @param password for the user
     * @return <code>User</code> object if login is successful
     * @throws ConfigException
     */
    public UserAccount login( final String username,
                              final String password )
          throws ConfigException
    {
        if ( Utils.anyBlank( username, password ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        try
        {
            UserAccount user = null;

            if ( Validator.validEmail( username ) )
            {
                user = (UserAccount) em.createNamedQuery( "User.loginByEmail" )
                                       .setLockMode( LockModeType.NONE )
                                       .setParameter( "email", username )
                                       .getSingleResult();
            }
            else
            {
                user = (UserAccount) em.createNamedQuery( "User.loginByUsername" )
                                       .setLockMode( LockModeType.NONE )
                                       .setParameter( "username", username )
                                       .getSingleResult();
            }

            if ( null == user || !user.isActive() || !user.isPasswordValid( password ) )
            {
                throw new ConfigException( Error.Code.USER_AUTH );
            }

            return user;
        }
        catch ( NoResultException e )
        {
            throw new ConfigException( Error.Code.USER_AUTH );
        }
        catch ( Exception e )
        {
            handleException( e );
            throw new ConfigException( Error.Code.CATCH_ALL );
        }
    }


    public boolean isEmailRegistered( final String email )
    {
        long count = (long) em.createNamedQuery( "User.isRegistered" )
                              .setParameter( "email", email )
                              .setLockMode( LockModeType.NONE )
                              .getSingleResult();
        return count > 0;
    }


    public boolean isUsernameTaken( final String username )
    {
        long count = (long) em.createNamedQuery( "User.isUsernameTaken" )
                              .setLockMode( LockModeType.NONE )
                              .setParameter( "username", username )
                              .getSingleResult();
        return count > 0;
    }


    // --------------------------------------------------------------------------------------------
    // Organization
    // --------------------------------------------------------------------------------------------
    public Organization createOrganization( final String accountName,
                                            final UserAccount owner )
          throws ConfigException
    {
        Organization organization = new Organization();
        organization.setAccountName( accountName );
        organization.addOwner( owner );

        saveOrUpdateNonAudited( organization );
        return organization;
    }


    public void deleteOrganization( Organization organization,
                                    final UserAccount owner )
          throws ConfigException
    {
        if ( Utils.anyNull( organization, owner ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !organization.isOwner( owner ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        if ( organization.getRepositoryCount() > 0 )
        {
            throw new ConfigException( Error.Code.ORG_DELETE );
        }

        delete( organization );
    }


    public Organization update( final Organization organization,
                                final UserAccount owner )
          throws ConfigException
    {
        if ( Utils.anyNull( organization, owner ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        // ToDo:: SECURITY RISK
        // Someone could add a user to the admins or owners, and then make change to the repository.
        // This code would find that user in this non-persisted / modified repository and would
        // persist the change.
        //
        // We have to get the repository from the database and validate ownershit/admin-ship through that
        // object.  Not through modified one.
        if ( !organization.isOwner( owner ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        saveOrUpdateNonAudited( organization );
        return organization;
    }


    public Organization removeOwner( final Organization organization,
                                     final UserAccount owner )
          throws ConfigException
    {
        if ( Utils.anyNull( organization, owner ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !organization.isOwner( owner ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        organization.removeOwner( owner );

        saveOrUpdateNonAudited( organization );
        return organization;
    }


    public Organization removeAdministrator( final Organization organization,
                                             final UserAccount owner )
          throws ConfigException
    {
        if ( Utils.anyNull( organization, owner ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !organization.isOwner( owner ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        organization.removeAdministrator( owner );

        saveOrUpdateNonAudited( organization );
        return organization;
    }

    // --------------------------------------------------------------------------------------------
    // Repository
    // --------------------------------------------------------------------------------------------


    /**
     * @param user
     * @param repository
     * @param searchTerm
     * @return Map<PropertyKey                                                               ,                                                                                                                               Collection                                                               <                                                               Property>> of keys and values that contain the searchTerm
     * @throws ConfigException
     */
    public Map<PropertyKey, Collection<Property>> searchKeysAndValues( final UserAccount user,
                                                                       final Repository repository,
                                                                       final Date dateObj,
                                                                       final String searchTerm )
          throws ConfigException
    {
        if ( Utils.anyNull( repository ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.hasReadAccess( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        List<PropertyKey> keys = null;
        List<Property> props = null;

        if ( null == dateObj )
        {
            try
            {
                keys = em.createNamedQuery( "Search.keysAndComments" )
                         .setLockMode( LockModeType.NONE )
                         .setParameter( "repository", repository )
                         .setParameter( "searchTerm", "%" + searchTerm.toUpperCase() + "%" )
                         .getResultList();
            }
            catch ( NoResultException ignore )
            {
            }
            catch ( Exception e )
            {
                handleException( e );
            }

            try
            {
                props = em.createNamedQuery( "Search.values" )
                          .setLockMode( LockModeType.NONE )
                          .setParameter( "repository", repository )
                          .setParameter( "searchTerm", "%" + searchTerm.toUpperCase() + "%" )
                          .getResultList();
            }
            catch ( NoResultException ignore )
            {
            }
            catch ( Exception e )
            {
                handleException( e );
            }
        }
        else
        {
            AuditReader reader = AuditReaderFactory.get( em );
            Number rev = reader.getRevisionNumberForDate( dateObj );

            AuditQuery query = reader.createQuery().forEntitiesAtRevision( PropertyKey.class, rev );
            query.add( AuditEntity.property( "repository" ).eq( repository ) );
            query.add( AuditEntity.or( AuditEntity.property( "key" ).ilike( "%" + searchTerm + "%" ),
                                       AuditEntity.property( "readme" ).ilike( "%" + searchTerm + "%" ) ) );

            keys = query.getResultList();

            query = reader.createQuery().forEntitiesAtRevision( Property.class, rev );
            query.add( AuditEntity.property( "repository" ).eq( repository ) );
            query.add( AuditEntity.property( "value" ).ilike( "%" + searchTerm + "%" ) );

            props = query.getResultList();
        }

        Map<PropertyKey, Collection<Property>> keyListMap = new HashMap<>();
        if ( null != keys )
        {
            keys.forEach( k -> keyListMap.put( k, null ) );
        }

        if ( null != props )
        {
            props.forEach( p -> {
                PropertyKey key = p.getPropertyKey();
                if ( keyListMap.containsKey( key ) )
                {
                    Collection<Property> ps = keyListMap.get( key );
                    if ( null == ps )
                    {
                        ps = new ArrayList<>();
                        keyListMap.put( key, ps );
                    }
                    ps.add( p );
                }
                else
                {
                    ArrayList<Property> ps = new ArrayList<>();
                    ps.add( p );
                    keyListMap.put( key, ps );
                }
            } );
        }
        return keyListMap;
    }


    /**
     * Personal repository
     *
     * @param name        for the repository.  Has to be unique
     * @param description for the repository.  Optional
     * @param depth       specifies hierarchy for context elements
     * @param owner       User that created the repository
     * @param depthLabels if different from default
     * @return <code>Repository</code> created, if successful
     * @throws ConfigException
     */
    public Repository createRepository( final String name,
                                        final String description,
                                        final Depth depth,
                                        final boolean isPrivate,
                                        final UserAccount owner,
                                        final Map<Depth, String> depthLabels,
                                        final boolean confirmContextChange,
                                        final boolean isCachingEnabled )
          throws ConfigException
    {
        Repository repository = new Repository( name, depth, isPrivate, owner.getAccount() );
        repository.setDescription( description );
        repository.setDepthLabels( depthLabels );
        repository.setSecurityProfilesEnabled( true );
        repository.setValueTypeEnabled( true );
        repository.setConfirmContextChange( confirmContextChange );
        repository.setCachingEnabled( isCachingEnabled );

        saveOrUpdateAudited( owner, repository, repository );
        return repository;
    }

    /**
     * Repository creation that belongs to the organization
     *
     * @param name
     * @param description
     * @param depth
     * @param isPrivate
     * @param organization
     * @param depthLabels
     * @param author
     * @return
     * @throws ConfigException
     */
    public Repository createRepository( final String name,
                                        final String description,
                                        final Depth depth,
                                        final boolean isPrivate,
                                        final Organization organization,
                                        final Map<Depth, String> depthLabels,
                                        final UserAccount author,
                                        final boolean confirmContextChange,
                                        final boolean isCachingEnabled )
            throws ConfigException
    {
        // ToDo:: SECURITY RISK
        // Someone could add a user to the admins or owners, and then make change to the repository.
        // This code would find that user in this non-persisted / modified repository and would
        // persist the change.
        //
        // We have to get the repository from the database and validate ownershit/admin-ship through that
        // object.  Not through modified one.
        if ( !organization.isOwner( author ) && !organization.isAdmin( author ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        Repository repository = new Repository( name, depth, isPrivate, organization.getAccount() );
        repository.setDescription( description );
        repository.setDepthLabels( depthLabels );
        repository.setSecurityProfilesEnabled( true );
        repository.setValueTypeEnabled( true );
        repository.setConfirmContextChange( confirmContextChange );
        repository.setCachingEnabled( isCachingEnabled );

        saveOrUpdateAudited( author, repository, repository );

        return repository;
    }

    /**
     * @param repository to delete
     * @param owner      that is deleting the repository
     * @return true if deleted
     * @throws ConfigException is thrown if user is not the owner
     */
    public boolean deleteRepository( final Repository repository,
                                     final UserAccount owner )
          throws ConfigException
    {
        if ( Utils.anyNull( repository, owner ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.isOwner( owner ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        Account account = repository.getAccount();
        if ( account.isPersonal() )
        {
            account.removeRepository( repository );
        }
        else
        {
            for ( UserAccount user : account.getOrganization().getOwners() )
            {
                user.getAccount().removeRepository( repository );
            }

            for ( UserAccount user : account.getOrganization().getAdministrators() )
            {
                user.getAccount().removeRepository( repository );
            }

            account.removeRepository( repository );
        }

        repository.destroy();
        saveOrUpdateAudited( owner, repository, repository );
        saveOrUpdateNonAudited( account );

        return true;
    }


    public Repository updateDepthLabels( final Repository repository,
                                         final UserAccount user,
                                         String[] labels )
          throws ConfigException
    {
        if ( Utils.anyNull( repository, user, labels ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.isEditableBy( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        int i = 0;
        for ( Depth d : repository.getDepth().getDepths() )
        {
            repository.setDepthLabel( d, labels[i++] );
        }

        saveOrUpdateAudited( user, repository, repository );
        return repository;
    }


    /**
     * @param repository that needs to be updated
     * @param author     that made the change
     * @return Repository that was updated
     * @throws ConfigException is thrown if unable to persist
     */
    public Repository update( final Repository repository,
                              final UserAccount author )
          throws ConfigException
    {
        if ( Utils.anyNull( repository, author ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.isEditableBy( author ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        saveOrUpdateAudited( author, repository, repository );
        return repository;
    }


    /**
     * @param repository
     * @param currentOwner
     * @param newOwner
     * @return
     * @throws ConfigException
     */
    public Repository transferOwnership( final Repository repository,
                                         final UserAccount currentOwner,
                                         final Account newOwner )
          throws ConfigException
    {
        if ( Utils.anyNull( repository, currentOwner, newOwner ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.isOwner( currentOwner ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        repository.transferOwnershipAccount( newOwner, currentOwner );
        saveOrUpdateAudited( currentOwner, repository, repository );
        return repository;
    }


    public List<Repository> getAllRepositories()
    {
        try
        {
            return em.createNamedQuery( "Repository.getAll" )
                     .setLockMode( LockModeType.NONE )
                     .getResultList();
        }
        catch ( NoResultException e )
        {
            return null;
        }
        catch ( Exception e )
        {
            handleException( e );
        }

        return null;
    }


    /**
     * @param accountName
     * @param repositoryName
     * @return
     * @throws ConfigException
     */
    public Repository getRepository( final String accountName,
                                     final String repositoryName )
          throws ConfigException
    {
        if ( Utils.anyNull( accountName, repositoryName ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        try
        {
            Account pn = (Account) em.createNamedQuery( "AccountName.get" )
                                     .setLockMode( LockModeType.NONE )
                                     .setParameter( "name", accountName )
                                     .getSingleResult();

            return (Repository) em.createNamedQuery( "Repository.getByAccount" )
                                  .setLockMode( LockModeType.NONE )
                                  .setParameter( "name", repositoryName )
                                  .setParameter( "account", pn )
                                  .getSingleResult();
        }
        catch ( NoResultException e )
        {
            return null;
        }
        catch ( Exception e )
        {
            handleException( e );
        }

        return null;
    }


    /**
     * @param repositoryId
     * @param date
     * @return
     * @throws ConfigException
     */
    public Repository getRepository( final Long repositoryId,
                                     final Date date )
          throws ConfigException
    {
        if ( Utils.anyNull( repositoryId ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( null == date )
        {
            return get( Repository.class, repositoryId );
        }

        AuditReader reader = AuditReaderFactory.get( em );
        Number rev = reader.getRevisionNumberForDate( date );

        AuditQuery query = reader.createQuery().forEntitiesAtRevision( Repository.class, rev );
        query.add( AuditEntity.id().eq( repositoryId ) );

        try
        {
            return (Repository) query.getSingleResult();
        }
        catch ( NoResultException e )
        {
            return null;
        }
        catch ( Exception e )
        {
            handleException( e );
            return null;
        }
    }


    /**
     * @param repository
     * @param user
     * @param label
     * @param insertIndex
     * @return
     * @throws ConfigException
     */
    public Repository expandContextScope( final Repository repository,
                                          final UserAccount user,
                                          String label,
                                          int insertIndex )
          throws ConfigException
    {
        if ( Utils.anyNull( repository, user, label ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.isAdminOrOwner( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        if ( insertIndex < 0 || insertIndex > 10 )
        {
            throw new ConfigException( Error.Code.CONTEXT_SCOPE_MISMATCH ); // ToDo:  different error
        }

        int currIndex = repository.getDepth().getIndex();
        if ( currIndex == 10 )
        {
            throw new ConfigException( Error.Code.CONTEXT_SCOPE_MISMATCH ); // ToDo:  max scope size
        }

        // widest scope
        if ( insertIndex == 0 )
        {
            // No additional work needed
            Depth newDepth = Depth.getByIndex( currIndex + 1 );
            repository.setDepth( newDepth );

            Map<Depth, String> depthLabels = repository.getDepthLabels();
            depthLabels.put( newDepth, label );
            repository.setDepthLabels( depthLabels );
        }
        // most specific scope
        else if ( currIndex == insertIndex / 2 )
        {
            // Got to shift entire context block and re-context all properties.
            int indent = insertIndex / 2;

            Depth depth = repository.getDepth();
            Map<Depth, String> depthLabels = repository.getDepthLabels();

            for ( Depth d : depth.getDepths() )
            {
                Depth newDepth = Depth.getByIndex( d.getIndex() + 1 );
                depthLabels.put( newDepth, repository.getLabel( d ) );

                if ( --indent == 0 )
                {
                    depthLabels.put( d, label );
                    break;
                }
            }

            Depth newRepoDepth = Depth.getByIndex( currIndex + 1 );
            repository.setDepth( newRepoDepth );
            repository.setDepthLabels( depthLabels );

            for ( Depth d : depth.getDepths() )
            {
                Depth newDepth = Depth.getByIndex( d.getIndex() + 1 );
                Collection<CtxLevel> ctxLevels = getLevelsForDepth( repository, user, d );

                ctxLevels.stream().forEach( l -> {
                    l.setDepth( newDepth );
                    saveOrUpdateAudited( user, repository, l );
                } );
            }
        }
        // middle scope
        else
        {
            // Got to shift entire context block and re-context all properties.
            int indent = insertIndex / 2;

            Depth depth = repository.getDepth();
            Map<Depth, String> depthLabels = repository.getDepthLabels();

            for ( Depth d : depth.getDepths() )
            {
                Depth newDepth = Depth.getByIndex( d.getIndex() + 1 );
                Collection<CtxLevel> ctxLevels = getLevelsForDepth( repository, user, d );

                depthLabels.put( newDepth, repository.getLabel( d ) );

                ctxLevels.forEach(l -> {
                    l.setDepth( newDepth );
                    saveOrUpdateAudited( user, repository, l );
                } );

                if ( --indent == 0 )
                {
                    depthLabels.put( d, label );
                    break;
                }
            }

            Depth newDepth = Depth.getByIndex( currIndex + 1 );
            repository.setDepth( newDepth );
            repository.setDepthLabels( depthLabels );
        }

        repository.getProperties().forEach(AContextAwarePersistent::updateContextString);
        repository.getFiles().forEach(AContextAwarePersistent::updateContextString);
        saveOrUpdateAudited( user, repository, repository );

        log.info( "Inserting context hierarchy: " + label + " index: " + insertIndex );

        return repository;
    }


    /**
     * @param repository
     * @param user
     * @param depthToRemove
     * @return
     * @throws ConfigException
     */
    public Repository removeContextRank( final Repository repository,
                                         final UserAccount user,
                                         final Depth depthToRemove )
          throws ConfigException
    {
        if ( Utils.anyNull( repository, user, depthToRemove ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.isAdminOrOwner( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        int currIndex = repository.getDepth().getIndex();

        if ( currIndex == 1 )
        {
            throw new ConfigException( Error.Code.CONTEXT_SCOPE_MISMATCH ); // ToDo:  max scope size
        }

        Collection<CtxLevel> ctxLevels = getLevelsForDepth( repository, user, depthToRemove );
        if ( null != ctxLevels && ctxLevels.size() > 0 )
        {
            if ( ctxLevels.stream().anyMatch( l -> null != l.getProperties() && l.getProperties().size() > 0 ) )
            {
                throw new ConfigException( Error.Code.ASSIGNED_PROPERTIES );
            }
        }

        // remove all levels for depth that is being removed
        ctxLevels.stream().forEach( l -> deleteAudited( user, repository, l ) );

        int removeIndex = depthToRemove.getIndex();

        for ( int depthIndex = removeIndex + 1; depthIndex <= currIndex; depthIndex++ )
        {
            Depth oldDepth = Depth.getByIndex( depthIndex );
            Depth newDepth = Depth.getByIndex( depthIndex - 1 );

            ctxLevels = getLevelsForDepth( repository, user, oldDepth );
            ctxLevels.stream().forEach( l -> {
                l.setDepth( newDepth );
                saveOrUpdateAudited( user, repository, l );
            } );
        }

        Map<Depth, String> depthLabelMap = repository.getDepthLabels();
        for ( int depthIndex = removeIndex; depthIndex < currIndex; depthIndex++ )
        {
            Depth depth = Depth.getByIndex( depthIndex );
            Depth widerDepth = Depth.getByIndex( depthIndex + 1 );

            depthLabelMap.put( depth, depthLabelMap.get( widerDepth ) );
        }
        depthLabelMap.remove( Depth.getByIndex( currIndex ) );

        // new repository depth
        Depth newDepth = Depth.getByIndex( currIndex - 1 );

        repository.setDepth( newDepth );
        repository.setDepthLabels( depthLabelMap );
        repository.getProperties().stream().forEach( p -> p.updateContextString() );
        repository.getFiles().stream().forEach( f -> f.updateContextString() );

        saveOrUpdateAudited( user, repository, repository );
        return repository;
    }


    // --------------------------------------------------------------------------------------------
    // Rules
    // --------------------------------------------------------------------------------------------
    public boolean deleteRule( Repository repository,
                               UserAccount user,
                               Long ruleId )
          throws ConfigException
    {
        if ( Utils.anyNull( repository, user, ruleId ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.isAdminOrOwner( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        return deleteAudited( user, repository, repository.deleteRule( ruleId ) );
    }

    // --------------------------------------------------------------------------------------------
    // ClientRequest
    // --------------------------------------------------------------------------------------------


    // --------------------------------------------------------------------------------------------
    // Tokens
    // --------------------------------------------------------------------------------------------
    public void saveToken( final UserAccount user,
                           final Token token )
          throws ConfigException
    {
        if ( null == token )
        {
            return;
        }

        saveOrUpdateAudited( user, token.getRepository(), token );
    }


    // ToDo: depending on ownership, make sure permissions are there
    public void deleteToken( final Repository repository,
                             final UserAccount user,
                             final Long tokenId )
          throws ConfigException
    {
        if ( Utils.anyNull( repository, user, tokenId ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.hasWriteAccess( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        Token tk = getToken( repository, user, tokenId );
        if ( null == tk )
        {
            return;
        }

        if ( !tk.isAllowedToDelete( user ) )
        {
            throw new ConfigException( Error.Code.NOT_AUTHORIZED_TO_CHANGE_TOKEN );
        }

        deleteAudited( user, repository, tk );
    }


    public Collection<Token> getTokens( final Repository repository )
          throws ConfigException
    {
        if ( null == repository )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        try
        {
            return em.createNamedQuery( "Token.getAll" )
                     .setLockMode( LockModeType.NONE )
                     .setParameter( "repository", repository )
                     .getResultList();
        }
        catch ( NoResultException e )
        {
            return null;
        }
        catch ( Exception e )
        {
            handleException( e );
        }

        return null;
    }


    public Token getToken( final Repository repository,
                           final String token )
          throws ConfigException
    {
        if ( null == repository || Utils.isBlank( token ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        try
        {
            return (Token) em.createNamedQuery( "Token.getToken" )
                             .setLockMode( LockModeType.NONE )
                             .setParameter( "repository", repository )
                             .setParameter( "token", token )
                             .getSingleResult();
        }
        catch ( NoResultException e )
        {
            return null;
        }
        catch ( Exception e )
        {
            handleException( e );
        }

        return null;
    }


    public Token getToken( final Repository repository,
                           final UserAccount user,
                           final Long tokenId )
          throws ConfigException
    {
        if ( Utils.anyNull( repository, tokenId ) )
        {
            return null;
        }

        if ( !repository.hasReadAccess( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        try
        {
            return (Token) em.createNamedQuery( "Token.byId" )
                             .setLockMode( LockModeType.NONE )
                             .setParameter( "repository", repository )
                             .setParameter( "id", tokenId )
                             .getSingleResult();
        }
        catch ( NoResultException e )
        {
            return null;
        }
        catch ( Exception e )
        {
            handleException( e );
        }

        return null;
    }

    // --------------------------------------------------------------------------------------------
    // Level
    // --------------------------------------------------------------------------------------------


    /**
     * @param name  for the new level
     * @param depth contextual hierarchy
     * @param user  created by
     * @return Level create
     * @throws ConfigException is thrown if unable to persist
     */
    public CtxLevel createLevel( final String name,
                                 final Depth depth,
                                 final UserAccount user,
                                 final Repository repository )
          throws ConfigException
    {
        if ( !repository.hasWriteAccess( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        if ( !repository.canUserManageContext( user ) )
        {
            throw new ConfigException( Error.Code.CONTEXT_EDIT_DISABLED );
        }

        CtxLevel ctxLevel = new CtxLevel( repository, depth );
        ctxLevel.setType( CtxLevel.LevelType.Standalone );
        ctxLevel.setName( name );

        if ( null != user )
        {
            saveOrUpdateAudited( user, repository, ctxLevel );
        }

        return ctxLevel;
    }


    /**
     * @param name
     * @param depth
     * @param repository
     * @param appIdentifier
     * @param changeComment
     * @return
     * @throws ConfigException
     */
    public CtxLevel createLevelViaApi( final String name,
                                       final Depth depth,
                                       final Repository repository,
                                       final String appIdentifier,
                                       final String changeComment )
          throws ConfigException
    {
        CtxLevel ctxLevel = new CtxLevel( repository, depth );
        ctxLevel.setType( CtxLevel.LevelType.Standalone );
        ctxLevel.setName( name );

        saveOrUpdateAuditedViaAPI( appIdentifier, repository, ctxLevel, changeComment );

        return ctxLevel;
    }


    public CtxLevel createNonPersistedLevel( final String name,
                                             final Depth depth,
                                             final Repository repository )
          throws ConfigException
    {
        CtxLevel ctxLevel = new CtxLevel( repository, depth );
        ctxLevel.setName( name );
        ctxLevel.setType( CtxLevel.LevelType.Standalone );

        return ctxLevel;
    }


    /**
     * Gets level specified by id, that belongs to the repository.
     *
     * @param levelId    if of the level
     * @param repository to which it belongs
     * @return Level, or null if level does not exist.
     * @throws ConfigException is thrown if unable to persist
     */
    public CtxLevel getLevel( final Long levelId,
                              final Repository repository )
          throws ConfigException
    {
        if ( Utils.anyNull( levelId, repository ) )
        {
            return null;
        }

        try
        {
            return (CtxLevel) em.createNamedQuery( "Level.byId" )
                                .setLockMode( LockModeType.NONE )
                                .setParameter( "id", levelId )
                                .setParameter( "repository", repository )
                                .getSingleResult();
        }
        catch ( NoResultException e )
        {
            return null;
        }
        catch ( Exception e )
        {
            handleException( e );
        }

        return null;
    }


    /**
     * Get levels as a map where the key is a depth.  This method will always return
     * a non-null map, with every depth used by a repository.  Each Depth key will have a
     * non-null Collection of Level objects, which might be empty.
     *
     * @param repository to which levels as assigned
     * @return Map<Depth                               ,                                                               Collection                               <                               Level>>
     * @throws ConfigException if unable to fetch from the database
     */
    public Map<Depth, Collection<CtxLevel>> getLevelsByDepth( final Repository repository )
          throws ConfigException
    {
        if ( null == repository )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        Map<Depth, Collection<CtxLevel>> levelMap = new HashMap<>();

        for ( Depth depth : repository.getDepth().getDepths() )
        {
            levelMap.put( depth, new HashSet<>() );
        }

        for ( CtxLevel ctxLevel : repository.getCtxLevels() )
        {
            levelMap.get( ctxLevel.getDepth() ).add( ctxLevel );
        }

        return levelMap;
    }


    public Collection<CtxLevel> getLevelsForDepth( final Repository repository,
                                                   final UserAccount user,
                                                   final Depth depth )
          throws ConfigException
    {
        if ( null == repository )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.hasReadAccess( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        try
        {
            return em.createNamedQuery( "Level.getForDepth" )
                     .setLockMode( LockModeType.NONE )
                     .setParameter( "repository", repository )
                     .setParameter( "depth", depth )
                     .getResultList();
        }
        catch ( NoResultException e )
        {
            return null;
        }
        catch ( Exception e )
        {
            handleException( e );
        }

        return null;
    }


    /**
     * Update level
     *
     * @param ctxLevel that is to be persisted with changed attributes
     * @param author   that is making the change
     * @return the same level after it was persisted
     * @throws ConfigException is throw if unable to save
     */
    @Deprecated
    // ToDo: remove
    public CtxLevel update( final CtxLevel ctxLevel,
                            final UserAccount author )
          throws ConfigException
    {
        if ( Utils.anyNull( ctxLevel, author ) )
        {
            return null;
        }

        if ( !ctxLevel.getRepository().hasWriteAccess( author ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        ctxLevel.getProperties().forEach( Property::updateContextString );
        ctxLevel.getFiles().forEach( RepoFile::updateContextString );

        saveOrUpdateAudited( author, ctxLevel.getRepository(), ctxLevel );
        return ctxLevel;
    }


    /**
     * @param repository
     * @param user
     * @param id
     * @param name
     * @param type
     * @param assignmentIds
     * @param depthLabel
     * @return
     * @throws ConfigException
     */
    public CtxLevel updateOrCreateLevel( final Repository repository,
                                         final UserAccount user,
                                         final Long id,
                                         final String name,
                                         final CtxLevel.LevelType type,
                                         final Collection<Long> assignmentIds,
                                         final String depthLabel )
          throws ConfigException
    {
        if ( Utils.anyNull( repository, name ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.hasWriteAccess( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        if ( !repository.canUserManageContext( user ) )
        {
            throw new ConfigException( Error.Code.CONTEXT_EDIT_DISABLED );
        }


        Depth depth = repository.getDepthFromLabel( depthLabel );
        CtxLevel ctxLevel;

        boolean updatePropertyContextStrings = false;
        boolean isNew = id == null;
        boolean levelTypeChanged = false;

        if ( !isNew )
        {
            ctxLevel = getLevel( id, repository );

            if ( null == ctxLevel )
            {
                throw new ConfigException( Error.Code.NOT_FOUND );
            }

            updatePropertyContextStrings = !ctxLevel.getName().equals( name );
            ctxLevel.setName( name );
            levelTypeChanged = !ctxLevel.getType().equals( type );
        }
        else
        {
            ctxLevel = new CtxLevel( repository, depth );
            ctxLevel.setName( name );
        }

        // when do the property contexts need to be re-written ?
        // - if a Level type changes to Group
        // - if a Level type changes from Group
        // - if level name has changed

        updatePropertyContextStrings |= levelTypeChanged && ( CtxLevel.LevelType.Group.equals( type ) || CtxLevel.LevelType.Group
              .equals
                    ( ctxLevel
                            .getType() ) );


        // Check access to the edited level
        AccessRuleWrapper accessRuleWrapper = null;
        boolean accessControlled = repository.isAccessControlEnabled();
        if ( accessControlled )
        {
            accessRuleWrapper = repository.getRulesWrapper( user );

            if ( !isLevelModificationAllowed( accessRuleWrapper, accessControlled, ctxLevel ) )
            {
                throw new ConfigException( Error.Code.LEVEL_EDITING_ACCESS_DENIED, ctxLevel.toJson() );
            }
        }


        // Collect all level assignments;
        // Check write access to them
        // Make sure they are assignable to this level
        Set<CtxLevel> assignments = new HashSet<>();
        if ( null != assignmentIds )
        {
            for ( Long lid : assignmentIds )
            {
                CtxLevel l = getLevel( lid, repository );
                if ( null == l )
                {
                    throw new ConfigException( Error.Code.NOT_FOUND );
                }

                // ToDo, this should be a better check - not just that type is same
                if ( l.getType().equals( type ) )
                {
                    throw new ConfigException( Error.Code.GROUP_TO_GROUP_ASSIGNMENT, l.toJson() );
                }

                if ( !isLevelModificationAllowed( accessRuleWrapper, accessControlled, l ) )
                {
                    throw new ConfigException( Error.Code.LEVEL_EDITING_ACCESS_DENIED, l.toJson() );
                }

                assignments.add( l );
            }
        }

        // If this is a new level, just save and return
        if ( isNew )
        {
            ctxLevel.setType( type );
            if ( CtxLevel.LevelType.Group == type )
            {
                ctxLevel.setMembers( assignments );
            }
            else if ( CtxLevel.LevelType.Member == type )
            {
                assignments.forEach( group -> group.addMember( ctxLevel ) );
            }

            // ToDo: should we save assignments

            saveOrUpdateAudited( user, repository, ctxLevel );
            return ctxLevel;
        }


        // This is an edited level, so...

        // Type of the level has not changed.
        if ( !levelTypeChanged )
        {

            if ( CtxLevel.LevelType.Group == type )
            {
                ctxLevel.setMembers( assignments );
            }
            else if ( CtxLevel.LevelType.Member == type )
            {
                Set<CtxLevel> currentGroups = ctxLevel.getGroups();

                if ( null != currentGroups )
                {
                    // get groups that should not longer have level as a member
                    currentGroups.removeAll( assignments );
                    currentGroups.forEach( group -> group.removeMember( ctxLevel ) );
                }

                // refresh assignments
                assignments.forEach( group -> group.addMember( ctxLevel ) );
            }
            else if ( CtxLevel.LevelType.Standalone == type )
            {
                ctxLevel.setMembers( null );
            }

            // ToDo: should we save assignments

            saveOrUpdateAudited( user, repository, ctxLevel );

            if ( updatePropertyContextStrings )
            {
                if ( null != ctxLevel.getProperties() )
                {
                    ctxLevel.getProperties().forEach( Property::updateContextString );
                }

                if ( null != ctxLevel.getFiles() )
                {
                    ctxLevel.getFiles().forEach( RepoFile::updateContextString );
                }
            }

            return ctxLevel;
        }


        // Level type has changed
        switch ( ctxLevel.getType() )
        {
            case Group:
            {
                ctxLevel.setMembers( null );
                if ( CtxLevel.LevelType.Member == type )
                {
                    assignments.forEach( group -> group.addMember( ctxLevel ) );
                }

                break;
            }

            case Member:
            {
                Set<CtxLevel> currentGroups = ctxLevel.getGroups();
                if ( null != currentGroups )
                {
                    currentGroups.forEach( group -> group.removeMember( ctxLevel ) );
                }

                if ( CtxLevel.LevelType.Group == type )
                {
                    ctxLevel.setMembers( assignments );
                }

                break;
            }

            case Standalone:
            {
                if ( CtxLevel.LevelType.Group == type )
                {
                    ctxLevel.setMembers( assignments );
                }

                else if ( CtxLevel.LevelType.Member == type )
                {
                    assignments.forEach( group -> group.addMember( ctxLevel ) );
                }

                break;
            }
        }

        ctxLevel.setType( type );
        if ( updatePropertyContextStrings )
        {
            if ( null != ctxLevel.getProperties() )
            {
                ctxLevel.getProperties().forEach( Property::updateContextString );
            }
            if ( null != ctxLevel.getFiles() )
            {
                ctxLevel.getFiles().forEach( RepoFile::updateContextString );
            }
        }

        saveOrUpdateAudited( user, repository, ctxLevel );

        return ctxLevel;
    }


    private boolean isLevelModificationAllowed( final AccessRuleWrapper accessRuleWrapper,
                                                boolean accessControlled,
                                                final CtxLevel ctxLevel )
          throws ConfigException
    {
        if ( accessControlled )
        {
            if ( null == accessRuleWrapper )
            {
                throw new ConfigException( Error.Code.LEVEL_EDITING_ACCESS_DENIED );
            }

            if ( null != ctxLevel.getProperties() )
            {
                ctxLevel.getProperties().stream().forEach( p -> {
                    accessRuleWrapper.executeRuleFor( p );
                    if ( !p.isEditable )
                    {
                        throw new ConfigException( Error.Code.LEVEL_EDITING_ACCESS_DENIED );
                    }
                } );
            }
        }

        return true;
    }


    /**
     * Delete level
     *
     * @param user       that is deleting the level
     * @param repository to which level belongs
     * @param ctxLevel   to be deleted
     * @return true if deleted
     * @throws ConfigException is throw if unable to delete
     */
    public boolean deleteLevel( final UserAccount user,
                                final Repository repository,
                                final CtxLevel ctxLevel )
          throws ConfigException
    {
        if ( !repository.hasWriteAccess( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        if ( null != ctxLevel.getProperties() && ctxLevel.getProperties().size() > 0 )
        {
            throw new ConfigException( Error.Code.ASSIGNED_PROPERTIES );
        }

        return deleteAudited( user, repository, ctxLevel );
    }

    // --------------------------------------------------------------------------------------------
    // Level Auditing
    // --------------------------------------------------------------------------------------------


    /**
     * Get levels at a specific point in time.
     *
     * @param repository To which levels are assigned
     * @param time       At the time levels were current
     * @return Collection of Levels from that time
     * @throws ConfigException Is thrown if there were problems fetching records
     */
    public Collection<CtxLevel> getLevels( final Repository repository,
                                           final Date time )
          throws ConfigException
    {
        AuditReader reader = AuditReaderFactory.get( em );
        Number rev = reader.getRevisionNumberForDate( time );

        AuditQuery query = reader.createQuery().forEntitiesAtRevision( CtxLevel.class, rev );
        query.add( AuditEntity.property( "repository" ).eq( repository ) );

        return query.getResultList();
    }


    public CtxLevel getLevel( final String levelName,
                              final Depth depth,
                              final Repository repository,
                              final Date time )
          throws ConfigException
    {
        if ( Utils.anyNull( levelName, repository ) )
        {
            return null;
        }

        if ( null == time )
        {
            try
            {
                String levelUpper = levelName.toUpperCase();

                return (CtxLevel) em.createNamedQuery( "Level.getByName" )
                                    .setLockMode( LockModeType.NONE )
                                    .setParameter( "repository", repository )
                                    .setParameter( "name", levelUpper )
                                    .setParameter( "depth", depth )
                                    .getSingleResult();
            }
            catch ( NoResultException e )
            {
                return null;
            }
            catch ( Exception e )
            {
                handleException( e );
            }

            return null;
        }


        AuditReader reader = AuditReaderFactory.get( em );
        Number rev = reader.getRevisionNumberForDate( time );

        // ToDo will not return deleted level
        AuditQuery query = reader.createQuery().forEntitiesAtRevision( CtxLevel.class, rev );
        query.add( AuditEntity.property( "repository" ).eq( repository ) );
        query.add( AuditEntity.property( "name" ).eq( levelName ) );
        query.add( AuditEntity.property( "depth" ).eq( depth ) );

        try
        {
            return (CtxLevel) query.getSingleResult();
        }
        catch ( NoResultException e )
        {
            return null;
        }
        catch ( Exception e )
        {
            handleException( e );
            return null;
        }
    }


    /**
     * Get levels at specific time.
     *
     * @param repository
     * @param time
     * @return
     * @throws ConfigException
     */
    public Map<Depth, Collection<CtxLevel>> getLevelsByDepth( final Repository repository,
                                                              final Date time )
          throws ConfigException
    {
        Map<Depth, Collection<CtxLevel>> levelMap = new HashMap<>();

        for ( Depth depth : repository.getDepth().getDepths() )
        {
            levelMap.put( depth, new HashSet<>() );
        }

        for ( CtxLevel ctxLevel : getLevels( repository, time ) )
        {
            levelMap.get( ctxLevel.getDepth() ).add( ctxLevel );
        }

        return levelMap;
    }

    // --------------------------------------------------------------------------------------------
    // Property
    // --------------------------------------------------------------------------------------------


    public Property getProperty( final UserAccount user,
                                 final Repository repository,
                                 final Long propertyId )
          throws ConfigException
    {
        if ( Utils.anyNull( user, repository, propertyId ) )
        {
            return null;
        }

        if ( !repository.hasWriteAccess( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        try
        {
            return (Property) em.createNamedQuery( "Property.get" )
                                .setLockMode( LockModeType.NONE )
                                .setParameter( "repository", repository )
                                .setParameter( "id", propertyId )
                                .getSingleResult();
        }
        catch ( NoResultException e )
        {
            return null;
        }
        catch ( Exception e )
        {
            handleException( e );
            return null;
        }
    }


    public void savePropertyKey( final String appIdentity,
                                 final Repository repository,
                                 final Token token,
                                 final PropertyKey propertyKey,
                                 final String changeComment )
          throws ConfigException
    {
        validateWriteAccess( repository, token, propertyKey );
        saveOrUpdateAuditedViaAPI( appIdentity, repository, propertyKey, changeComment );
    }


    public void saveProperty( final String appIdentity,
                              final Repository repository,
                              final Token token,
                              final Property property,
                              final String changeComment )
          throws ConfigException
    {
        validateWriteAccess( repository, token, property );
        saveOrUpdateAuditedViaAPI( appIdentity, repository, property, changeComment );
    }


    /**
     * Create and persist new Property object
     *
     * @param key
     * @param value
     * @param active
     * @param readme
     * @param deprecated
     * @param context
     * @param changeComment
     * @param spPassword
     * @param spName
     * @param repository
     * @param user
     * @return created Property
     * @throws ConfigException
     */
    public Property createProperty( final String key,
                                    final String value,
                                    final boolean active,
                                    final String readme,
                                    final boolean deprecated,
                                    final boolean push,
                                    final PropertyKey.ValueDataType valueDataType,
                                    final Collection<CtxLevel> context,
                                    final String changeComment,
                                    final String spPassword,
                                    final String spName,
                                    final Repository repository,
                                    final UserAccount user )
          throws ConfigException
    {
        if ( Utils.allNull( user, repository ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.hasWriteAccess( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        Property property = new Property( repository );

        PropertyKey propertyKey = getKey( repository, key );
        if ( null == propertyKey )
        {
            propertyKey = createPropertyKey( user, repository, key, readme, deprecated, valueDataType, push );

            if ( !Utils.isBlank( spName ) )
            {
                SecurityProfile sp = getSecurityProfile( user, repository, null, spName );
                if ( null == sp )
                {
                    throw new ConfigException( Error.Code.MISSING_SECURITY_PROFILE );
                }

                propertyKey.setSecurityProfile( sp, spPassword );
            }
        }

        property.setPropertyKey( propertyKey );
        propertyKey.addProperty( property );

        switch ( propertyKey.getValueDataType() )
        {
            case FileEmbed:
            case FileRef:
                if ( null == value )
                {
                    property.setAbsoluteFilePath( null );
                    break;
                }

                AbsoluteFilePath absoluteFilePath = getAbsFilePath( repository, value, null );
                if ( null == absoluteFilePath )
                {
                    throw new ConfigException( Error.Code.FILE_NOT_FOUND );
                }

                property.setAbsoluteFilePath( absoluteFilePath );
                break;

            default:
                property.setValue( value, spPassword );
                break;
        }


        property.setActive( active );
        property.setContext( context );

        validateWriteAccess( repository, user, property );
        saveOrUpdateAudited( user, repository, property, changeComment );
        return property;
    }


    /**
     * Update property and key.  If this property was the last one assigned to the old key,
     * than, part of this change will be the removal of the old key from the database.
     *
     * @param propertyId to be updated
     * @param key        if null or blank, key will not be changed
     * @param readme     for the key
     * @param user       that is making the change
     * @return Property that was updated
     * @throws ConfigException is thrown if unable to make the change
     */
    public Property update( final Long propertyId,
                            final String key,
                            final String readme,
                            final boolean deprecated,
                            final boolean pushEnabled,
                            final PropertyKey.ValueDataType valueDataType,
                            final String changeComment,
                            final boolean active,
                            final UserAccount user )
          throws ConfigException
    {
        // ToDo this method is only used by the unit-test.  It should be removed.

        if ( Utils.anyNull( propertyId, user ) )
        {
            return null;
        }

        Property property = get( Property.class, propertyId );
        final Repository repository = property.getRepository();

        if ( !repository.hasWriteAccess( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        String oldKey = property.getKey();

        PropertyKey propertyKey = getKey( repository, key );
        if ( null == propertyKey )
        {
            propertyKey = createPropertyKey( user, repository, key, readme, deprecated, valueDataType, pushEnabled );
        }

        propertyKey.setReadme( readme );
        property.setPropertyKey( propertyKey );
        property.setActive( active );

        validateWriteAccess( repository, user, property );
        saveOrUpdateAudited( user, repository, property, changeComment );

        PropertyKey oldPropertyKey = getKey( repository, oldKey );
        if ( oldPropertyKey.getProperties().size() == 0 )
        {
            deleteAudited( user, repository, oldPropertyKey, changeComment );
        }

        return property;
    }


    /**
     * @param propertyId
     * @param value
     * @param context
     * @param changeComment
     * @param active
     * @param secretKey
     * @param user
     * @return
     * @throws ConfigException
     */
    public Property updateProperty( final Long propertyId,
                                    final String value,
                                    final Collection<CtxLevel> context,
                                    final String changeComment,
                                    final boolean active,
                                    final String secretKey,
                                    final UserAccount user )
          throws ConfigException
    {
        if ( Utils.anyNull( propertyId ) )
        {
            throw new ConfigException( ( Error.Code.MISSING_PARAMS ) );
        }

        Property property = get( Property.class, propertyId );
        if ( null == property )
        {
            throw new ConfigException( Error.Code.RESOURCE_NOT_FOUND );
        }

        Repository repository = property.getRepository();

        if ( !repository.hasWriteAccess( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        switch ( property.getPropertyKey().getValueDataType() )
        {
            case FileEmbed:
            case FileRef:
                if ( null == value )
                {
                    property.setAbsoluteFilePath( null );
                    break;
                }

                AbsoluteFilePath absoluteFilePath = getAbsFilePath( repository, value, null );
                if ( null == absoluteFilePath )
                {
                    throw new ConfigException( Error.Code.FILE_NOT_FOUND );
                }

                property.setAbsoluteFilePath( absoluteFilePath );
                break;

            default:
                property.setValue( value, secretKey );
                break;
        }

        property.setContext( context );
        property.setActive( active );

        validateWriteAccess( repository, user, property );

        saveOrUpdateAudited( user, repository, property, changeComment );
        return property;
    }


    /**
     * Delete unused keys via multi-key selected cleanup UI.
     *
     * @param user
     * @param repository
     * @param keys
     * @return
     * @throws ConfigException
     */
    public boolean deleteUnusedKeys( final UserAccount user,
                                     final Repository repository,
                                     final List<String> keys )
          throws ConfigException
    {
        if ( Utils.anyNull( repository, keys ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.hasWriteAccess( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        for ( String keyString : keys )
        {
            PropertyKey key = getKey( repository, keyString.trim() );
            if ( null == key || key.isSecure() )
            {
                continue;
            }

            if ( null != key.getFiles() && key.getFiles().size() > 0 )
            {
                throw new ConfigException( Error.Code.KEY_USED_BY_FILES );
            }

            if ( null != key.getProperties() && key.getProperties().size() > 0 )
            {
                throw new ConfigException( Error.Code.KEY_USED_BY_VALUES );
            }

            validateWriteAccess( repository, user, key );
            deleteAudited( user, repository, key );
        }

        return true;
    }


    /**
     * @param user
     * @param repository
     * @param keyString
     * @param password
     * @return
     * @throws ConfigException
     */
    public boolean deleteKeyAndProperties( final UserAccount user,
                                           final Repository repository,
                                           final String keyString,
                                           final String password )
          throws ConfigException
    {
        if ( Utils.anyNull( repository, keyString ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.hasWriteAccess( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        PropertyKey key = getKey( repository, keyString );
        if ( null == key )
        {
            throw new ConfigException( Error.Code.RESOURCE_NOT_FOUND );
        }

        if ( key.isSecure() && !key.getSecurityProfile().isSecretValid( password ) )
        {
            throw new ConfigException( Error.Code.INVALID_PASSWORD );
        }

        if ( null != key.getFiles() && key.getFiles().size() > 0 )
        {
            throw new ConfigException( Error.Code.KEY_USED_BY_FILES );
        }

        validateWriteAccess( repository, user, key );
        Iterator<Property> itt = key.getProperties().iterator();

        while ( itt.hasNext() )
        {
            Property property = itt.next();
            itt.remove();

            validateWriteAccess( repository, user, property );
            deleteAudited( user, repository, property );
        }

        deleteAudited( user, repository, key );
        return true;
    }


    public boolean deleteKeyAndProperties( final String appIdentity,
                                           final Repository repository,
                                           final Token token,
                                           final String keyString,
                                           final String password,
                                           final String changeComment )
          throws ConfigException
    {
        if ( Utils.anyNull( repository, keyString ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        PropertyKey key = getKey( repository, keyString );
        if ( null == key )
        {
            return true;
        }

        if ( key.isSecure() && !key.getSecurityProfile().isSecretValid( password ) )
        {
            throw new ConfigException( Error.Code.INVALID_PASSWORD );
        }

        if ( null != key.getFiles() && key.getFiles().size() > 0 )
        {
            throw new ConfigException( Error.Code.KEY_USED_BY_FILES );
        }

        validateWriteAccess( repository, token, key );
        Iterator<Property> itt = key.getProperties().iterator();

        while ( itt.hasNext() )
        {
            Property property = itt.next();
            itt.remove();

            deleteAuditedViaAPI( appIdentity, repository, property, changeComment );
        }

        deleteAuditedViaAPI( appIdentity, repository, key, changeComment );
        return true;
    }


    /**
     * Delete the property.  If this property was the last one assigned to the key,
     * then part of this change will be the removal of the property key as well.
     *
     * @param user       User deleting this property
     * @param propertyId to be deleted
     * @return true if deleted, otherwise false
     * @throws ConfigException is thrown if unable to delete property
     */
    public boolean deleteProperty( final UserAccount user,
                                   final Repository repository,
                                   final Long propertyId,
                                   final String password )
          throws ConfigException
    {
        if ( Utils.anyNull( propertyId, repository ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.hasWriteAccess( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        Property property = getProperty( user, repository, propertyId );
        if ( null == property )
        {
            return true;
        }

        validateWriteAccess( repository, user, property );

        if ( property.isSecure() )
        {
            if ( !property.getPropertyKey().getSecurityProfile().isSecretValid( password ) )
            {
                throw new ConfigException( Error.Code.INVALID_PASSWORD );
            }
        }

        String key = property.getKey();

        boolean ret;
        ret = deleteAudited( user, repository, property );
        PropertyKey propertyKey = getKey( repository, key );

        if ( propertyKey.getProperties().size() == 0 && ( null == propertyKey.getFiles() || propertyKey.getFiles()
                                                                                                       .size() == 0 ) )
        {
            ret &= deleteAudited( user, repository, propertyKey );
        }

        return ret;
    }


    public boolean deleteProperty( final String appIdentity,
                                   final Repository repository,
                                   final Token token,
                                   final Long propertyId,
                                   final String password,
                                   final String changeComment )
          throws ConfigException
    {
        if ( Utils.anyNull( propertyId, repository ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        Property property = get( Property.class, propertyId );
        if ( null == property )
        {
            return true;
        }

        validateWriteAccess( repository, token, property );

        String key = property.getKey();

        boolean ret;
        ret = deleteAuditedViaAPI( appIdentity, repository, property, changeComment );

        PropertyKey propertyKey = getKey( repository, key );

        if ( propertyKey.isSecure() && !propertyKey.getSecurityProfile().isSecretValid( password ) )
        {
            throw new ConfigException( Error.Code.INVALID_PASSWORD );
        }

        if ( propertyKey.getProperties().size() == 0 && ( null == propertyKey.getFiles() || propertyKey.getFiles()
                                                                                                       .size() == 0 ) )
        {
            ret &= deleteAuditedViaAPI( appIdentity, repository, propertyKey, changeComment );
        }

        return ret;
    }


    /**
     * Get properties at a specific point in time.
     *
     * @param repository To which levels are assigned
     * @param date       At the time levels were current
     * @return Collection of Levels from that time
     * @throws ConfigException Is thrown if there were problems fetching records
     */
    public Collection<Property> getProperties( final Repository repository,
                                               final Date date )
          throws ConfigException
    {
        AuditReader reader = AuditReaderFactory.get( em );
        Number rev = reader.getRevisionNumberForDate( date );

        AuditQuery query = reader.createQuery().forEntitiesAtRevision( Property.class, rev );
        query.add( AuditEntity.property( "repository" ).eq( repository ) );

        return query.getResultList();
    }


    public Pair<PropertyKey, Collection<Property>> getPropertiesForKey( final Repository repository,
                                                                        final Date date,
                                                                        String key )
          throws ConfigException
    {
        AuditReader reader = AuditReaderFactory.get( em );
        Number rev = reader.getRevisionNumberForDate( null == date ? new Date() : date );

        AuditQuery kq = reader.createQuery().forEntitiesAtRevision( PropertyKey.class, rev );
        kq.add( AuditEntity.property( "repository" ).eq( repository ) );
        kq.add( AuditEntity.property( "key" ).eq( key ) );

        PropertyKey propertyKey;
        try
        {
            propertyKey = (PropertyKey) kq.getSingleResult();
        }
        catch ( NoResultException e )
        {
            return null;
        }

        AuditQuery query = reader.createQuery().forEntitiesAtRevision( Property.class, rev );
        query.add( AuditEntity.property( "repository" ).eq( repository ) );
        query.add( AuditEntity.relatedId( "propertyKey" ).eq( propertyKey.getId() ) );

        Collection<Property> properties = query.getResultList();
        propertyKey.propertyCount = properties.size();

        return new Pair( propertyKey, properties );
    }

    // --------------------------------------------------------------------------------------------
    // PropertyKey
    // --------------------------------------------------------------------------------------------


    /**
     * Create new property key
     *
     * @param author     that is creating it
     * @param repository where the key belongs
     * @param key        of this property key
     * @param readme     optional readme for the key
     * @return PropertyKey that was created
     * @throws ConfigException
     */
    private PropertyKey createPropertyKey( final UserAccount author,
                                           final Repository repository,
                                           final String key,
                                           final String readme,
                                           final boolean deprecated,
                                           final PropertyKey.ValueDataType valueDataType,
                                           final boolean pushEnabled )
          throws ConfigException
    {
        if ( Utils.allNull( author, repository ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        PropertyKey propertyKey = new PropertyKey( repository, key, valueDataType );
        propertyKey.setReadme( readme );
        propertyKey.setDeprecated( deprecated );
        propertyKey.setPushValueEnabled( pushEnabled );

        saveOrUpdateAudited( author, repository, propertyKey );
        return propertyKey;
    }


    /**
     * Key the key for a repository with a specific String key
     *
     * @param repository for the key
     * @param key        of the propertyKey
     * @return null if key does not exist, otherwise, PropertyKey object
     */
    public PropertyKey getKey( final Repository repository,
                               final String key )
    {
        return getKey( repository, key, null );
    }


    /**
     * Returns the key from a specific date
     *
     * @param repository
     * @param key
     * @param date
     * @return
     */
    public PropertyKey getKey( final Repository repository,
                               final String key,
                               final Date date )
    {
        if ( null == repository || Utils.isBlank( key ) )
        {
            return null;
        }

        try
        {
            if ( null == date )
            {
                return (PropertyKey) em.createNamedQuery( "Key.getByKey" )
                                       .setLockMode( LockModeType.NONE )
                                       .setParameter( "key", key.toUpperCase() )
                                       .setParameter( "repository", repository )
                                       .getSingleResult();
            }

            AuditReader reader = AuditReaderFactory.get( em );
            Number rev = reader.getRevisionNumberForDate( null == date ? new Date() : date );

            AuditQuery kq = reader.createQuery().forEntitiesAtRevision( PropertyKey.class, rev );
            kq.add( AuditEntity.property( "repository" ).eq( repository ) );
            kq.add( AuditEntity.property( "key" ).eq( key ) );

            return (PropertyKey) kq.getSingleResult();
        }
        catch ( NoResultException e )
        {
            return null;
        }
        catch ( Exception e )
        {
            handleException( e );
            return null;
        }
    }


    public enum KeyUpdateStatus
    {
        UPDATE, RENAME, MERGE
    }


    /**
     * @param repository
     * @param originalKeyString
     * @param newKeyString
     * @param comment
     * @param user
     * @return
     * @throws ConfigException
     */
    public Pair<PropertyKey, KeyUpdateStatus> updatePropertyKey( final Repository repository,
                                                                 final String originalKeyString,
                                                                 final String newKeyString,
                                                                 final String vdt,
                                                                 final String comment,
                                                                 final boolean deprecated,
                                                                 final boolean push,
                                                                 final UserAccount user,
                                                                 final String spName,
                                                                 final String spPassword,
                                                                 final String currentPassword,
                                                                 final String changeComment )
          throws ConfigException
    {
        if ( Utils.anyNull( repository, originalKeyString ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.hasWriteAccess( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        PropertyKey.ValueDataType valueDataType = PropertyKey.ValueDataType.Text;
        if ( repository.isValueTypeEnabled() )
        {
            try
            {
                valueDataType = PropertyKey.ValueDataType.valueOf( vdt );
            }
            catch ( Exception e )
            {
                throw new ConfigException( Error.Code.INVALID_VALUE_DATA_TYPE );
            }
        }

        PropertyKey originalKey = getKey( repository, originalKeyString );

        // New Key
        if ( null == originalKey )
        {
            PropertyKey propertyKey = new PropertyKey( repository, newKeyString, valueDataType );
            propertyKey.setReadme( comment );
            propertyKey.setDeprecated( deprecated );
            propertyKey.setPushValueEnabled( push );

            if ( !Utils.isBlank( spName ) )
            {
                SecurityProfile sp = getSecurityProfile( user, repository, null, spName );
                if ( null == sp )
                {
                    throw new ConfigException( Error.Code.MISSING_SECURITY_PROFILE );
                }

                propertyKey.setSecurityProfile( sp, spPassword );
            }

            validateWriteAccess( repository, user, propertyKey );
            saveOrUpdateAudited( user, repository, propertyKey, changeComment );

            return new Pair<>( propertyKey, KeyUpdateStatus.UPDATE );
        }

        validateWriteAccess( repository, user, originalKey );

        KeyUpdateStatus status;
        PropertyKey toSave;
        PropertyKey toDelete = null;

        // Key has changed.  Move all properties to the new key
        if ( !originalKey.getKey().equals( newKeyString ) )
        {
            PropertyKey newKey = getKey( repository, newKeyString );

            // keys are not merging - newKey does not exist
            if ( null == newKey )
            {
                originalKey.getFiles().parallelStream().forEach( f -> f.updateKey( originalKey, newKeyString, null ) );

                originalKey.setKey( newKeyString );
                originalKey.setReadme( comment );
                originalKey.setDeprecated( deprecated );
                status = KeyUpdateStatus.RENAME;
                toSave = originalKey;
            }
            // keys are merging
            else
            {
                if ( repository.isSecurityProfilesEnabled() && !Utils.same( originalKey.getSecurityProfile(),
                                                                            newKey.getSecurityProfile() ) )
                {
                    throw new ConfigException( Error.Code.ENCRYPTION_MISMATCH );
                }

                if ( !Utils.same( originalKey.getValueDataType(), newKey.getValueDataType() ) )
                {
                    throw new ConfigException( Error.Code.VALUE_DATA_TYPE_MISMATCH );
                }

                // ToDo, merging files scenarios
                originalKey.getFiles().parallelStream().forEach( f -> f.updateKey( originalKey, newKeyString, newKey ) );

                newKey.setReadme( comment );
                newKey.setDeprecated( deprecated );
                status = KeyUpdateStatus.MERGE;

                List<Property> props = new ArrayList<>( originalKey.getProperties() );
                Iterator<Property> itt = props.iterator();

                while ( itt.hasNext() )
                {
                    Property property = itt.next();

                    Property movedProperty = new Property( repository );
                    movedProperty.setPropertyKey( newKey );
                    newKey.addProperty( movedProperty );

                    movedProperty.setValue( property.getValue(), spPassword );
                    movedProperty.setActive( property.isActive() );
                    movedProperty.setContext( property.getContext() );
                }

                toSave = newKey;
                toDelete = originalKey;
            }
        }
        // Just update existing propertyKey
        else
        {
            originalKey.setReadme( comment );
            originalKey.setDeprecated( deprecated );

            status = UPDATE;
            toSave = originalKey;
        }

        if ( repository.isValueTypeEnabled() && null != toSave.getValueDataType() && !valueDataType.equals( toSave
                                                                                                                  .getValueDataType() ) )
        {
            PropertyKey.ValueDataType oldDataType = toSave.getValueDataType();
            PropertyKey.ValueDataType newDataType = valueDataType;

            SecurityProfile sp = repository.isSecurityProfilesEnabled() && !Utils.isBlank( spName ) ? getSecurityProfile(
                  user,
                  repository,
                  null,
                  spName ) : null;

            toSave.setValueDataType( newDataType );

            toSave.getProperties().stream().forEach( property -> {
                String value = ( null == sp || !sp.encryptionEnabled() )
                               ? property.getValue()
                               : sp.decrypt( property.getValue(), currentPassword );

                if ( null == value )
                {
                    property.setValue( null, currentPassword );
                }
                else
                {

                    switch ( oldDataType )
                    {
                        case JSON:
                        {
                            switch ( newDataType )
                            {
                                case Text:
                                case Code:
                                case JSON:
                                    break;

                                case Map:
                                    property.setValue( value, currentPassword );
                                    break;

                                case List:
                                    property.setValue( value, currentPassword );
                                    break;

                                case Boolean:
                                case Integer:
                                case Long:
                                case Double:
                                case Float:
                                    throw new ConfigException( Error.Code.VALUE_DATA_TYPE_CONVERSION );

                                case FileEmbed:
                                case FileRef:
                                    AbsoluteFilePath absoluteFilePath = getAbsFilePath( repository, value, null );
                                    if ( null != absoluteFilePath )
                                    //                                    throw new ConfigException(Error.Code
                                    // .FILE_NOT_FOUND);

                                    //                                else
                                    {
                                        property.setAbsoluteFilePath( absoluteFilePath );
                                    }
                                    break;
                            }
                            break;
                        }

                        case Text:
                        case Code:
                        {
                            switch ( newDataType )
                            {
                                case Text:
                                case Code:
                                case JSON:
                                    break;

                                case Boolean:
                                    property.setValue( Utils.textToBoolean( value ), currentPassword );
                                    break;

                                case Integer:
                                    property.setValue( Utils.textToInteger( value ), currentPassword );
                                    break;

                                case Long:
                                    property.setValue( Utils.textToLong( value ), currentPassword );
                                    break;

                                case Double:
                                    property.setValue( Utils.textToDouble( value ), currentPassword );
                                    break;

                                case Float:
                                    property.setValue( Utils.textToFloat( value ), currentPassword );
                                    break;

                                case Map:
                                {
                                    property.setValue( Utils.textToJsonMap( value ), currentPassword );
                                    break;
                                }
                                case List:
                                    property.setValue( Utils.textToJsonList( value ), currentPassword );
                                    break;

                                case FileEmbed:
                                case FileRef:
                                    AbsoluteFilePath absoluteFilePath = getAbsFilePath( repository, value, null );
                                    if ( null != absoluteFilePath )
                                    //                                    throw new ConfigException(Error.Code
                                    // .FILE_NOT_FOUND);
                                    //                                else
                                    {
                                        property.setAbsoluteFilePath( absoluteFilePath );
                                    }
                                    break;
                            }
                            break;
                        }
                        case Boolean:
                        {
                            switch ( newDataType )
                            {
                                case Boolean:
                                    break;

                                case List:
                                    property.setValue( Utils.textToJsonList( value ), currentPassword );
                                    break;

                                case Map:
                                case Integer:
                                case Long:
                                case Double:
                                case Float:
                                case JSON:
                                case FileEmbed:
                                case FileRef:
                                    throw new ConfigException( Error.Code.VALUE_DATA_TYPE_CONVERSION );

                                case Text:
                                case Code:
                                    break; // nothing to do
                            }

                            break;
                        }
                        case Integer:
                        case Long:
                        {
                            switch ( newDataType )
                            {
                                case Text:
                                case Code:
                                    break; // no need for conversion

                                case Boolean:
                                    property.setValue( "1".equals( value ) ? "true" : "false", currentPassword );
                                    break;

                                case Integer:
                                case Long:
                                case Double:
                                case Float:
                                    break; // nothing to do

                                case JSON:
                                case Map:
                                case FileEmbed:
                                case FileRef:
                                    throw new ConfigException( Error.Code.VALUE_DATA_TYPE_CONVERSION );

                                case List:
                                    property.setValue( Utils.textToJsonList( value ), currentPassword );
                                    break;
                            }
                            break;
                        }
                        case Double:
                        {
                            switch ( newDataType )
                            {
                                case Text:
                                case Code:
                                    break; // no need for conversion

                                case Boolean:
                                    property.setValue( "1".equals( value ) ? "true" : "false", currentPassword );
                                    break;

                                case Integer:
                                    property.setValue( Utils.textToInteger( value ), currentPassword );
                                    break;

                                case Long:
                                    property.setValue( Utils.textToLong( value ), currentPassword );
                                    break;

                                case Double:
                                    break;

                                case Float:
                                    property.setValue( Utils.textToFloat( value ), currentPassword );
                                    break;

                                case JSON:
                                case Map:
                                case FileEmbed:
                                case FileRef:
                                    throw new ConfigException( Error.Code.VALUE_DATA_TYPE_CONVERSION );

                                case List:
                                    property.setValue( Utils.textToJsonList( value ), currentPassword );
                                    break;
                            }
                            break;
                        }
                        case Float:
                        {
                            switch ( newDataType )
                            {
                                case Text:
                                case Code:
                                    break; // no need for conversion

                                case Boolean:
                                    property.setValue( "1".equals( value ) ? "true" : "false", currentPassword );
                                    break;

                                case Integer:
                                    property.setValue( Utils.textToInteger( value ), currentPassword );
                                    break;

                                case Long:
                                    property.setValue( Utils.textToLong( value ), currentPassword );
                                    break;

                                case Double:
                                    property.setValue( Utils.textToDouble( value ), currentPassword );
                                    break;

                                case Float:
                                    break;

                                case JSON:
                                case Map:
                                case FileEmbed:
                                case FileRef:
                                    throw new ConfigException( Error.Code.VALUE_DATA_TYPE_CONVERSION );

                                case List:
                                    property.setValue( Utils.textToJsonList( value ), currentPassword );
                                    break;
                            }
                            break;
                        }

                        case Map:
                        {
                            switch ( newDataType )
                            {
                                case Map:
                                    break;

                                case JSON:
                                    property.decryptValue( currentPassword );
                                    String v = property.getValue();

                                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                                    JsonObject obj = gson.fromJson( v, JsonObject.class );
                                    property.setValue( gson.toJson( obj ), currentPassword );

                                    break;

                                case Text:
                                case Code:
                                    property.setValue( Utils.jsonMapToText( value ), currentPassword );
                                    break;

                                case List:
                                    property.setValue( Utils.jsonMapToJsonList( value ), currentPassword );
                                    break;

                                case Boolean:
                                case Integer:
                                case Long:
                                case Double:
                                case Float:
                                case FileEmbed:
                                case FileRef:
                                    throw new ConfigException( Error.Code.VALUE_DATA_TYPE_CONVERSION );
                            }
                            break;
                        }

                        case List:
                        {
                            switch ( newDataType )
                            {
                                case List:
                                    break;

                                case JSON:
                                    property.decryptValue( currentPassword );
                                    String v = property.getValue();

                                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                                    JsonArray arr = gson.fromJson( v, JsonArray.class );
                                    property.setValue( gson.toJson( arr ), currentPassword );

                                    break;

                                case Text:
                                case Code:
                                    property.setValue( Utils.jsonListToText( value ), currentPassword );
                                    break;

                                case Map:
                                    property.setValue( Utils.jsonListToJsonMap( value ), currentPassword );
                                    break;

                                case Boolean:
                                case Integer:
                                case Long:
                                case Double:
                                case Float:
                                case FileEmbed:
                                case FileRef:
                                    throw new ConfigException( Error.Code.VALUE_DATA_TYPE_CONVERSION );
                            }
                            break;
                        }

                        case FileRef:
                        case FileEmbed:
                        {
                            switch ( newDataType )
                            {
                                case List:
                                case JSON:
                                case Map:
                                case Boolean:
                                case Integer:
                                case Long:
                                case Double:
                                case Float:
                                    throw new ConfigException( Error.Code.VALUE_DATA_TYPE_CONVERSION );

                                case Text:
                                case Code:
                                    property.setValue( property.getAbsoluteFilePath().getAbsPath(), currentPassword );
                                    break;

                                case FileEmbed:
                                case FileRef:
                                    property.enforce();
                                    break;
                            }
                            break;
                        }
                    }
                }
            } );
        }

        if ( toSave.isSecure() && !toSave.getSecurityProfile().isSecretValid( currentPassword ) )
        {
            throw new ConfigException( Error.Code.INVALID_PASSWORD );
        }

        if ( !Utils.isBlank( spName ) )
        {
            SecurityProfile sp = getSecurityProfile( user, repository, null, spName );
            if ( null == sp )
            {
                throw new ConfigException( Error.Code.RESOURCE_NOT_FOUND );
            }

            if ( !sp.equals( toSave.getSecurityProfile() ) )
            {
                if ( !sp.isSecretValid( spPassword ) )
                {
                    throw new ConfigException( Error.Code.INVALID_PASSWORD );
                }

                sp.sk = spPassword;
                toSave.setSecurityProfile( sp, currentPassword );
            }
        }
        else if ( toSave.isSecure() )
        {
            toSave.removeSecurityProfile( currentPassword );
        }
        toSave.setPushValueEnabled( push );

        validateWriteAccess( repository, user, toSave );

        saveOrUpdateAudited( user, repository, toSave, changeComment );
        if ( null != toDelete )
        {
            Iterator<Property> pii = toDelete.getProperties().iterator();
            while ( pii.hasNext() )
            {
                Property p = pii.next();
                pii.remove();
                toDelete.removeProperty( p );
                deleteAudited( user, repository, p, changeComment );
            }

            deleteAudited( user, repository, toDelete, changeComment );
        }
        return new Pair<>( toSave, status );
    }


    /**
     * @param searchTerm
     * @param max
     * @param repository
     * @return
     */
    public List<PropertyKey> searchKey( final String searchTerm,
                                        int max,
                                        Repository repository )
    {
        if ( null == repository || Utils.isBlank( searchTerm ) )
        {
            return null;
        }

        try
        {
            return em.createNamedQuery( "Key.search" )
                     .setLockMode( LockModeType.NONE )
                     .setParameter( "searchTerm", "%" + searchTerm.toUpperCase() + "%" )
                     .setParameter( "repository", repository )
                     .setMaxResults( max <= 0 ? 10 : ( max > 100 ? 100 : max ) )
                     .getResultList();
        }
        catch ( NoResultException e )
        {
            return null;
        }
        catch ( Exception e )
        {
            handleException( e );
            return null;
        }
    }


    public List<PropertyKey> nonTextTypeKeys( final Repository repository,
                                              final UserAccount user )
          throws ConfigException
    {
        if ( Utils.anyNull( user, repository ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.hasWriteAccess( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        try
        {
            return em.createNamedQuery( "Key.getNonText" )
                     .setLockMode( LockModeType.NONE )
                     .setParameter( "repository", repository )
                     .setParameter( "type", PropertyKey.ValueDataType.Text )
                     .getResultList();
        }
        catch ( NoResultException e )
        {
            return null;
        }
        catch ( Exception e )
        {
            handleException( e );
            return null;
        }
    }

    // --------------------------------------------------------------------------------------------
    // PropertyKey Auditing
    // --------------------------------------------------------------------------------------------


    public Collection<PropertyKey> getPropertiesKeys( final Repository repository,
                                                      final Date time )
          throws ConfigException
    {
        AuditReader reader = AuditReaderFactory.get( em );
        Number rev = reader.getRevisionNumberForDate( time );

        AuditQuery query = reader.createQuery().forEntitiesAtRevision( PropertyKey.class, rev );
        query.add( AuditEntity.property( "repository" ).eq( repository ) );

        return query.getResultList();
    }

    // --------------------------------------------------------------------------------------------
    // Teams
    // --------------------------------------------------------------------------------------------


    public Team updateTeam( final Repository repository,
                            final UserAccount user,
                            final Team team )
          throws ConfigException
    {
        if ( Utils.anyNull( user, repository, team ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.isAdminOrOwner( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        saveOrUpdateAudited( user, repository, team );
        return team;
    }


    public AccessRule updateAccessRule( final Repository repository,
                                        final UserAccount user,
                                        final AccessRule rule )
          throws ConfigException
    {
        if ( Utils.anyNull( user, repository, rule ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.isAdminOrOwner( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        saveOrUpdateAudited( user, repository, rule );
        return rule;
    }


    public Team setTeamUnmatchedEditable( final Repository repository,
                                          final UserAccount user,
                                          final String teamName,
                                          final boolean value )
          throws ConfigException
    {
        if ( Utils.anyNull( user, repository, teamName ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.isAdminOrOwner( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        Team team = getTeam( repository, teamName );
        if ( null == team )
        {
            throw new ConfigException( Error.Code.TEAM_NOT_FOUND );
        }

        team.setUnmatchedEditable( value );
        saveOrUpdateAudited( user, repository, team );

        return team;
    }


    public Team setStopOnFirstMatch( final Repository repository,
                                     final UserAccount user,
                                     final String teamName,
                                     final boolean value )
          throws ConfigException
    {
        if ( Utils.anyNull( user, repository, teamName ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.isAdminOrOwner( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        Team team = getTeam( repository, teamName );
        if ( null == team )
        {
            throw new ConfigException( Error.Code.TEAM_NOT_FOUND );
        }

        team.setStopOnFirstMatch( value );
        saveOrUpdateAudited( user, repository, team );

        return team;
    }


    private Team getTeam( final Repository repository,
                          final String name )
          throws ConfigException
    {
        try
        {
            return (Team) em.createNamedQuery( "Team.get" )
                            .setLockMode( LockModeType.NONE )
                            .setParameter( "repository", repository )
                            .setParameter( "name", name )
                            .getSingleResult();
        }
        catch ( NoResultException e )
        {
            return null;
        }
        catch ( Exception e )
        {
            handleException( e );
        }

        return null;
    }


    public Team getTeamForMember( final Repository repository,
                                  final UserAccount member )
          throws ConfigException
    {
        try
        {
            return (Team) em.createNamedQuery( "Team.forMember" )
                            .setLockMode( LockModeType.NONE )
                            .setParameter( "repository", repository )
                            .setParameter( "member", member )
                            .getSingleResult();
        }
        catch ( NoResultException e )
        {
            return null;
        }
        catch ( Exception e )
        {
            handleException( e );
        }

        return null;
    }


    public Team addTeamMember( final Repository repository,
                               final UserAccount user,
                               String teamName,
                               UserAccount newMember )
          throws ConfigException
    {
        if ( Utils.anyNull( user, repository, teamName, newMember ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.isAdminOrOwner( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        if ( null != getTeamForMember( repository, newMember ) )
        {
            throw new ConfigException( Error.Code.MULTIPLE_TEAM_MEMBERSHIPS );
        }

        Team team = getTeam( repository, teamName );
        if ( null == team )
        {
            throw new ConfigException( Error.Code.TEAM_NOT_FOUND );
        }

        team.addMember( newMember );
        newMember.getAccount().joinRepository( repository );

        saveOrUpdateAudited( user, repository, team );
        return team;
    }


    public Team removeTeamMember( final Repository repository,
                                  final UserAccount user,
                                  final UserAccount member )
          throws ConfigException
    {
        if ( Utils.anyNull( user, repository, member ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        // member can remove them-selves
        if ( !Objects.equals( user.getId(), member.getId() ) && !repository.isAdminOrOwner( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        Team team = getTeamForMember( repository, member );
        if ( null == team )
        {
            return null;
        }

        team.removeMember( member );
        saveOrUpdateAudited( user, repository, team );
        return team;
    }


    public void createTeam( final Repository repository,
                            final UserAccount user,
                            final String teamName )
          throws ConfigException
    {
        if ( Utils.anyNull( repository, user, teamName ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.isAdminOrOwner( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        Team team = new Team( repository, teamName );
        saveOrUpdateAudited( user, repository, team );
    }


    /**
     * @param team
     * @param user
     * @throws ConfigException
     */
    public void update( final Team team,
                        final UserAccount user )
          throws ConfigException
    {
        if ( Utils.anyNull( team, user ) )
        {
            return;
        }

        Repository repository = team.getRepository();
        if ( !repository.isAdminOrOwner( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        saveOrUpdateAudited( user, repository, team );
    }


    /**
     * @param team
     * @param ruleId
     * @return
     * @throws ConfigException
     */
    public AccessRule getRule( final Team team,
                               final Long ruleId )
          throws ConfigException
    {
        if ( Utils.anyNull( team, ruleId ) )
        {
            return null;
        }

        try
        {
            return (AccessRule) em.createNamedQuery( "Rule.byId" )
                                  .setLockMode( LockModeType.NONE )
                                  .setParameter( "team", team )
                                  .setParameter( "id", ruleId )
                                  .getSingleResult();
        }
        catch ( NoResultException e )
        {
            return null;
        }
        catch ( Exception e )
        {
            handleException( e );
            return null;
        }
    }


    /**
     * @param repository
     * @param user
     * @param teamName
     * @throws ConfigException
     */
    public void deleteTeam( final Repository repository,
                            final UserAccount user,
                            final String teamName )
          throws ConfigException
    {
        if ( Utils.anyNull( user, repository, teamName ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.isAdminOrOwner( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        Team team = getTeam( repository, teamName );
        if ( null == team )
        {
            return;
        }

        deleteAudited( user, repository, team );
    }


    public Team moveMemberToAnotherTeam( final Repository repository,
                                         final UserAccount user,
                                         final UserAccount member,
                                         final String toTeam )
          throws ConfigException
    {
        if ( Utils.anyNull( user, repository, member, toTeam ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.isAdminOrOwner( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        Team currentTeam = getTeamForMember( repository, member );
        if ( null != currentTeam )
        {
            currentTeam.removeMember( member );
            saveOrUpdateAudited( user, repository, currentTeam );
        }

        Team newTeam = addTeamMember( repository, user, toTeam, member );
        return newTeam;
    }

    // --------------------------------------------------------------------------------------------
    // SecurityProfile
    // --------------------------------------------------------------------------------------------


    /**
     * @param user
     * @param repository
     * @param spName
     * @return
     * @throws ConfigException
     */
    public SecurityProfile getSecurityProfile( final UserAccount user,
                                               final Repository repository,
                                               final Date date,
                                               final String spName )
          throws ConfigException
    {
        if ( Utils.anyNull( repository, spName ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.hasReadAccess( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        return getSecurityProfile( repository, date, spName );
    }


    public SecurityProfile getSecurityProfile( final Repository repository,
                                               final Date date,
                                               final String spName )
          throws ConfigException
    {
        try
        {
            if ( null == date )
            {
                return (SecurityProfile) em.createNamedQuery( "SecurityProfile.byName" )
                                           .setLockMode( LockModeType.NONE )
                                           .setParameter( "repository", repository )
                                           .setParameter( "name", spName )
                                           .getSingleResult();
            }

            AuditReader reader = AuditReaderFactory.get( em );
            Number rev = reader.getRevisionNumberForDate( date );

            AuditQuery kq = reader.createQuery().forEntitiesAtRevision( SecurityProfile.class, rev );
            kq.add( AuditEntity.property( "repository" ).eq( repository ) );
            kq.add( AuditEntity.property( "name" ).eq( spName ) );

            SecurityProfile sp = (SecurityProfile) kq.getSingleResult();
            return sp;
        }
        catch ( NoResultException e )
        {
            return null;
        }
        catch ( Exception e )
        {
            handleException( e );
        }

        return null;
    }


    public void updateSecureProfile( final UserAccount user,
                                     final Repository repository,
                                     final String profileName,
                                     final String newName,
                                     final String password,
                                     final String cipherName )
          throws ConfigException
    {
        if ( Utils.anyNull( user, repository, profileName, password ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        SecurityProfile sp = getSecurityProfile( user, repository, null, profileName );
        if ( null == sp )
        {
            throw new ConfigException( Error.Code.RESOURCE_NOT_FOUND );
        }

        if ( !sp.isSecretValid( password ) )
        {
            throw new ConfigException( Error.Code.INVALID_PASSWORD );
        }

        validateWriteAccess( repository, user, sp );

        if ( Utils.isBlank( newName ) )
        {
            throw new ConfigException( Error.Code.BLANK_NAME );
        }

        sp.setName( newName );

        CipherTransformation cipher = Utils.isBlank( cipherName ) ? null : CipherTransformation.get( cipherName );
        if ( !Utils.same( cipher, sp.getCipher() ) )
        {
            reCryptSecurityGroupMembers( sp, password, password, cipher );
        }

        saveOrUpdateAudited( user, repository, sp );
    }


    private void reCryptSecurityGroupMembers( final SecurityProfile sp,
                                              final String currentPassword,
                                              final String newPassword,
                                              final CipherTransformation cipher )
          throws ConfigException
    {
        if ( sp.encryptionEnabled() )
        {
            sp.getKeys().parallelStream().forEach( key -> {
                key.getProperties().parallelStream().forEach( p -> {
                    p.decryptValue( currentPassword );
                } );
            } );

            sp.getFiles().parallelStream().forEach( file -> {
                file.decryptFile( currentPassword );
            } );
        }

        sp.setCipher( cipher );
        if ( sp.encryptionEnabled() )
        {
            sp.getKeys().parallelStream().forEach( key -> key.getProperties()
                                                             .parallelStream()
                                                             .forEach( p -> p.encryptValue( newPassword ) ) );

            sp.getFiles().parallelStream().forEach( file -> {
                file.encryptFile( newPassword );
            } );
        }
    }


    public void updateSecurityGroupPassword( final UserAccount user,
                                             final Repository repository,
                                             final String securityGroupName,
                                             final String currentPassword,
                                             final String newPassword,
                                             final String userPassword )
          throws ConfigException
    {
        if ( Utils.anyNull( user, repository, securityGroupName, newPassword ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        SecurityProfile sp = getSecurityProfile( user, repository, null, securityGroupName );
        if ( null == sp )
        {
            throw new ConfigException( Error.Code.RESOURCE_NOT_FOUND );
        }

        validateWriteAccess( repository, user, sp );

        final String groupPassword;
        if ( Utils.isBlank( userPassword ) )
        {
            if ( !sp.isSecretValid( currentPassword ) )
            {
                throw new ConfigException( Error.Code.INVALID_PASSWORD );
            }

            groupPassword = currentPassword;
        }
        else
        {
            try
            {
                if ( !user.isPasswordValid( userPassword ) || !repository.isOwner( user ) )
                {
                    throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
                }
            }
            catch ( Exception ignore )
            {
                throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
            }

            groupPassword = sp.getDecodedPassword();
        }

        sp.setSecret( groupPassword, newPassword );
        reCryptSecurityGroupMembers( sp, groupPassword, newPassword, sp.getCipher() );

        saveOrUpdateAudited( user, repository, sp );
    }


    /**
     * @param user
     * @param repository
     * @param epName
     * @param password
     * @param cipher
     * @return
     * @throws ConfigException
     */
    public SecurityProfile createEncryptionProfile( final UserAccount user,
                                                    final Repository repository,
                                                    final String epName,
                                                    final String password,
                                                    final CipherTransformation cipher )
          throws ConfigException
    {
        if ( Utils.anyNull( repository, epName, password ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.hasWriteAccess( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        if ( !repository.isSecurityProfilesEnabled() )
        {
            throw new ConfigException( Error.Code.SECURITY_PROFILES_DISABLED );
        }

        SecurityProfile ep = new SecurityProfile( repository, cipher, epName, password );
        saveOrUpdateAudited( user, repository, ep );

        return ep;
    }


    /**
     * Delete security profile.  If profile was using encryption, all associated keys are decrypted
     * and saved before deletion.
     *
     * @param user
     * @param repository
     * @param name
     * @param password
     * @throws ConfigException
     */
    public void deleteSecurityProfile( final UserAccount user,
                                       final Repository repository,
                                       final String name,
                                       final String password )
          throws ConfigException
    {
        if ( Utils.anyNull( repository, name, password ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.hasWriteAccess( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        SecurityProfile sp = getSecurityProfile( user, repository, null, name );
        if ( null == sp )
        {
            throw new ConfigException( Error.Code.RESOURCE_NOT_FOUND );
        }

        if ( !sp.isSecretValid( password ) )
        {
            throw new ConfigException( Error.Code.INVALID_PASSWORD );
        }

        validateWriteAccess( repository, user, sp );

        sp.getKeys().parallelStream().forEach( key -> {
            key.removeSecurityProfile( password );
        } );

        deleteAudited( user, repository, sp );
    }


    private void validateWriteAccess( final Repository repository,
                                      final Token token,
                                      final AContextAwarePersistent contextAwarePersistent )
          throws ConfigException
    {
        if ( null == token )
        {
            if ( !repository.isAllowTokenFreeAPIPush() )
            {
                throw new ConfigException( Error.Code.TOKEN_FREE_PUSH_ACCESS_DENIED );
            }

            return;
        }

        if ( repository.isAccessControlEnabled() )
        {
            // If there are rules defined, make sure user has access to change the key.
            AccessRuleWrapper accessRuleWrapper = token.getRulesWrapper();

            // tokens, not assigned to a team, are wild-card (full access)
            if ( null == accessRuleWrapper )
            {
                return;
            }

            accessRuleWrapper.executeRuleFor( contextAwarePersistent );
            if ( !contextAwarePersistent.isEditable )
            {
                throw new ConfigException( Error.Code.CONTEXT_EDITING_ACCESS_DENIED );
            }
        }
    }


    private void validateWriteAccess( final Repository repository,
                                      final UserAccount user,
                                      final SecurityProfile sp )
          throws ConfigException
    {
        if ( repository.isAccessControlEnabled() )
        {
            // If there are rules defined, make sure user has access to change the key.
            AccessRuleWrapper accessRuleWrapper = repository.getRulesWrapper( user );

            if ( null == accessRuleWrapper )
            {
                throw new ConfigException( Error.Code.ACCESS_NOT_DEFINED );
            }

            sp.getKeys().forEach( k -> k.getProperties().forEach( p -> {
                accessRuleWrapper.executeRuleFor( p );
                if ( !p.isEditable )
                {
                    throw new ConfigException( Error.Code.KEY_EDITING_ACCESS_DENIED, p.toJson() );
                }
            } ) );
        }
    }


    private void validateWriteAccess( final Repository repository,
                                      final UserAccount user,
                                      final AContextAwarePersistent contextAwarePersistent )
          throws ConfigException
    {
        if ( repository.isAccessControlEnabled() )
        {
            // If there are rules defined, make sure user has access to change the key.
            AccessRuleWrapper accessRuleWrapper = repository.getRulesWrapper( user );

            if ( null == accessRuleWrapper )
            {
                throw new ConfigException( Error.Code.ACCESS_NOT_DEFINED );
            }

            accessRuleWrapper.executeRuleFor( contextAwarePersistent );
            if ( !contextAwarePersistent.isEditable )
            {
                throw new ConfigException( Error.Code.CONTEXT_EDITING_ACCESS_DENIED );
            }
        }
    }


    // Used by:
    // Store.deleteKeyAndProperties
    // Store.updatePropertyKey
    private void validateWriteAccess( final Repository repository,
                                      final UserAccount user,
                                      final PropertyKey propertyKey )
          throws ConfigException
    {
        if ( repository.isAccessControlEnabled() )
        {
            // If there are rules defined, make sure user has access to change the key.
            AccessRuleWrapper accessRuleWrapper = repository.getRulesWrapper( user );

            if ( null == accessRuleWrapper )
            {
                throw new ConfigException( Error.Code.ACCESS_NOT_DEFINED );
            }

            propertyKey.getProperties().forEach( p -> {
                accessRuleWrapper.executeRuleFor( p );
                if ( !p.isEditable )
                {
                    throw new ConfigException( Error.Code.CONTEXT_EDITING_ACCESS_DENIED, p.toJson() );
                }
            } );
        }
    }


    private void validateWriteAccess( final Repository repository,
                                      final Token token,
                                      final PropertyKey propertyKey )
          throws ConfigException
    {
        if ( null == token )
        {
            if ( !repository.isAllowTokenFreeAPIPush() )
            {
                throw new ConfigException( Error.Code.TOKEN_FREE_PUSH_ACCESS_DENIED );
            }

            return;
        }

        if ( repository.isAccessControlEnabled() )
        {
            // If there are rules defined, make sure user has access to change the key.
            AccessRuleWrapper accessRuleWrapper = token.getRulesWrapper();

            // tokens, not assigned to a team, are wild-card (full access)
            if ( null == accessRuleWrapper )
            {
                return;
            }

            propertyKey.getProperties().forEach( p -> {
                accessRuleWrapper.executeRuleFor( p );
                if ( !p.isEditable )
                {
                    throw new ConfigException( Error.Code.CONTEXT_EDITING_ACCESS_DENIED, p.toJson() );
                }
            } );
        }
    }


    public List<AuditRecord> getKeyAudit( final Repository repository,
                                          final UserAccount user,
                                          int max,
                                          final long starting,
                                          final int direction,
                                          final Long forUserId,
                                          final String forKey )
          throws ConfigException
    {
        if ( null == repository )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.isDemo() && null == user )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.hasReadAccess( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        try
        {
            StringBuilder hql = new StringBuilder();
            Map<String, Object> userParams = new HashMap<>();

            hql.append( "SELECT r FROM RevisionEntry r WHERE repositoryId = :repositoryId AND searchKey LIKE :searchKey" );
            userParams.put( "repositoryId", repository.getId() );
            userParams.put( "searchKey", "%|" + forKey + "|%" );

            if ( null != forUserId )
            {
                hql.append( " AND userId = :userId" );
                userParams.put( "userId", forUserId );
            }

            return getAuditCommits( getRevisions( max, starting, direction, hql.toString(), userParams ) );
        }
        catch ( NoResultException e )
        {
            return null;
        }
        catch ( Exception e )
        {
            handleException( e );
        }

        return null;
    }


    public List<AuditRecord> getCommitHistory( final Repository repository,
                                               final UserAccount user,
                                               int max,
                                               final long starting,
                                               final int direction,
                                               final Long forUserId,
                                               final boolean importantOnly,
                                               List<RevisionEntry.CommitGroup> commitGroup )
          throws ConfigException
    {
        if ( null == repository )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.isDemo() && null == user )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.hasReadAccess( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        if ( !importantOnly && null == commitGroup )
        {
            commitGroup = defaultCommitGroupList;
        }

        try
        {
            StringBuilder hql = new StringBuilder();
            Map<String, Object> userParams = new HashMap<>();

            hql.append( "SELECT r FROM RevisionEntry r WHERE repositoryId = :repositoryId" );
            userParams.put( "repositoryId", repository.getId() );

            if ( importantOnly )
            {
                hql.append( " AND notify = true" );
            }
            else
            {
                hql.append( " AND commitGroup IN (:commitGroup)" );
                userParams.put( "commitGroup", commitGroup );
            }

            if ( null != forUserId )
            {
                hql.append( " AND userId = :userId" );
                userParams.put( "userId", forUserId );
            }

            return getAuditCommits( getRevisions( max, starting, direction, hql.toString(), userParams ) );
        }
        catch ( NoResultException e )
        {
            return null;
        }
        catch ( Exception e )
        {
            handleException( e );
        }

        return null;
    }


    public List<AuditRecord> getCommit( final Repository repository,
                                        final UserAccount user,
                                        final Long revId )
          throws ConfigException
    {
        if ( null == repository )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.isDemo() && null == user )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.hasReadAccess( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        try
        {
            RevisionEntry revisionEntity = (RevisionEntry) em.createNamedQuery( "RevisionEntry.get" )
                                                             .setLockMode( LockModeType.NONE )
                                                             .setParameter( "repositoryId", repository.getId() )
                                                             .setParameter( "id", revId )
                                                             .getSingleResult();

            List<RevisionEntry> revs = new ArrayList<>();
            revs.add( revisionEntity );

            return getAuditCommits( revs );
        }
        catch ( NoResultException e )
        {
            return null;
        }
        catch ( Exception e )
        {
            handleException( e );
        }

        return null;
    }


    private List<RevisionEntry> getRevisions( int max,
                                              final long starting,
                                              final int direction,
                                              String baseHql,
                                              Map<String, Object> baseUserParams )
          throws ConfigException
    {
        if ( max > 100 )
        {
            max = 100;
        }
        else if ( max < 1 )
        {
            max = 10;
        }

        HashMap<String, Object> userParams = new HashMap<>(baseUserParams);
        StringBuilder hql = new StringBuilder();
        hql.append( baseHql );

        if ( 0 == starting && 0 == direction )
        {
            hql.append( " ORDER BY id DESC" );
        }
        else if ( direction > 0 )
        {
            hql.append( " AND id < :startingId ORDER BY id DESC" );
            userParams.put("startingId", starting);
        }
        else
        {
            hql.append( " AND id > :startingId ORDER BY id ASC" );
            userParams.put("startingId", starting);
        }

        List<RevisionEntry> revs = executeQuery( max, hql.toString(), userParams );
        if ( revs.size() < max && direction != 0  )
        {
            userParams = new HashMap<>( baseUserParams );
            hql = new StringBuilder();
            hql.append( baseHql );
            hql.append( " ORDER BY id " + (direction > 0 ? "ASC" : "DESC") );
            revs = executeQuery( max, hql.toString(), userParams );
        }

        return revs;
    }

    private List<RevisionEntry> executeQuery( int max,
                                              String queryString,
                                              Map<String, Object> userParams )
    {
        Query query = em.createQuery( queryString, RevisionEntry.class )
                        .setLockMode( LockModeType.NONE )
                        .setMaxResults( max );
        userParams.forEach( ( param, value ) -> query.setParameter( param, value ) );
        return query.getResultList();
    }


    private static List<RevisionEntry.CommitGroup> defaultCommitGroupList = new ArrayList<>();

    static
    {
        defaultCommitGroupList.add( RevisionEntry.CommitGroup.RepoSettings );
        defaultCommitGroupList.add( RevisionEntry.CommitGroup.Config );
        defaultCommitGroupList.add( RevisionEntry.CommitGroup.Files );
        defaultCommitGroupList.add( RevisionEntry.CommitGroup.Security );
        defaultCommitGroupList.add( RevisionEntry.CommitGroup.Tags );
        defaultCommitGroupList.add( RevisionEntry.CommitGroup.Tokens );
        defaultCommitGroupList.add( RevisionEntry.CommitGroup.Teams );
    }

    private List<AuditRecord> getAuditCommits( List<RevisionEntry> revs )
    {
        AuditReader reader = AuditReaderFactory.get( em );
        List<AuditRecord> changes = new ArrayList<>();

        for ( RevisionEntry rt : revs )
        {
            String[] clazzesSplit = rt.getType().split( "," );
            Set<Class> clazzes = new HashSet<>();
            for ( String cl : clazzesSplit )
            {
                clazzes.add( APersisted.ClassName.valueOf( cl ).getClazz() );
            }

            List<APersisted> results = new ArrayList<>();
            for ( Class clazz : clazzes )
            {
                AuditQuery query = reader.createQuery().forEntitiesModifiedAtRevision( clazz, rt.getId() );
                results.addAll( query.getResultList() );
            }
            changes.add( new AuditRecord( results, rt, get( UserAccount.class, rt.getUserId() ), rt.getAppId() ) );
        }

        return changes;
    }


    public void updateCommitComment( final UserAccount user,
                                     final Repository repository,
                                     long commitId,
                                     String comment )
          throws ConfigException
    {
        if ( Utils.anyNull( repository, user ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.isEditableBy( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        try
        {
            RevisionEntry re = (RevisionEntry) em.createNamedQuery( "RevisionEntry.get" )
                                                 .setLockMode( LockModeType.NONE )
                                                 .setParameter( "repositoryId", repository.getId() )
                                                 .setParameter( "id", commitId )
                                                 .getSingleResult();
            if ( null == re )
            {
                throw new ConfigException( Error.Code.MISSING_PARAMS );
            }

            re.setChangeComment( comment );
            saveOrUpdateNonAudited( re );
        }
        catch ( NoResultException e )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }
        catch ( Exception e )
        {
            handleException( e );
        }
    }


    /**
     * @param repositoryId
     * @param name
     * @return
     * @throws ConfigException
     */
    public Tag getTag( final Long repositoryId,
                       final String name )
          throws ConfigException
    {
        if ( Utils.anyNull( repositoryId, name ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        try
        {
            return (Tag) em.createNamedQuery( "Tag.getByName" )
                           .setLockMode( LockModeType.NONE )
                           .setParameter( "repositoryId", repositoryId )
                           .setParameter( "name", name )
                           .getSingleResult();
        }
        catch ( NoResultException e )
        {
            return null;
        }
        catch ( Exception e )
        {
            handleException( e );
            return null;
        }
    }


    public List<Tag> getTags( final Repository repository )
          throws ConfigException
    {
        if ( Utils.anyNull( repository ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        try
        {
            return em.createNamedQuery( "Tag.getAll" )
                     .setLockMode( LockModeType.NONE )
                     .setParameter( "repository", repository )
                     .getResultList();
        }
        catch ( NoResultException e )
        {
            return Collections.EMPTY_LIST;
        }
        catch ( Exception e )
        {
            handleException( e );
            return Collections.EMPTY_LIST;
        }
    }


    public long getUserCount()
          throws ConfigException
    {
        try
        {
            return (long) em.createNamedQuery( "User.count" )
                            .setLockMode( LockModeType.NONE )
                            .getSingleResult();
        }
        catch ( Exception e )
        {
            handleException( e );
            return 0;
        }
    }


    public long getRepositoryCount()
          throws ConfigException
    {
        try
        {
            return (long) em.createNamedQuery( "Repository.count" )
                            .setLockMode( LockModeType.NONE )
                            .getSingleResult();
        }
        catch ( Exception e )
        {
            handleException( e );
            return 0;
        }
    }


    public long getFileCount()
          throws ConfigException
    {
        try
        {
            return (long) em.createNamedQuery( "RepoFile.count" )
                            .setLockMode( LockModeType.NONE )
                            .getSingleResult();
        }
        catch ( Exception e )
        {
            handleException( e );
            return 0;
        }
    }


    public long getPropertyCount()
          throws ConfigException
    {
        try
        {
            return (long) em.createNamedQuery( "Property.count" )
                            .setLockMode( LockModeType.NONE )
                            .getSingleResult();
        }
        catch ( Exception e )
        {
            handleException( e );
            return 0;
        }
    }


    public Property getAuditProperty( final Repository repository,
                                      final UserAccount user,
                                      final long propertyId,
                                      final long revId )
          throws ConfigException
    {
        try
        {
            if ( !repository.hasReadAccess( user ) )
            {
                throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
            }

            AuditReader reader = AuditReaderFactory.get( em );
            AuditQuery query = reader.createQuery()
                                     .forRevisionsOfEntity( Property.class, true, true )
                                     .add( AuditEntity.revisionNumber().ge( revId ) )
                                     .add( AuditEntity.revisionNumber().le( revId ) )
                                     .add( AuditEntity.property( "repository" ).eq( repository ) )
                                     .add( AuditEntity.id().eq( propertyId ) );

            return (Property) query.getSingleResult();
        }
        catch ( NoResultException e )
        {
            return null;
        }
    }


    // ------------------------------------------------------------------------------------
    // File setters
    // ------------------------------------------------------------------------------------
    public List<AbsoluteFilePath> searchFile( final String searchTerm,
                                              int max,
                                              Repository repository )
    {
        if ( null == repository || Utils.isBlank( searchTerm ) )
        {
            return null;
        }

        try
        {
            return em.createNamedQuery( "AbsFilePath.searchByAbsPath" )
                     .setLockMode( LockModeType.NONE )
                     .setParameter( "absPath", "%" + searchTerm + "%" )
                     .setParameter( "repository", repository )
                     .setMaxResults( max <= 0 ? 10 : ( max > 100 ? 100 : max ) )
                     .getResultList();
        }
        catch ( NoResultException e )
        {
            return null;
        }
        catch ( Exception e )
        {
            handleException( e );
            return null;
        }
    }


    public RepoFile createRepoFile( final UserAccount user,
                                    final Repository repository,
                                    final String path,
                                    final String fileName,
                                    final String content,
                                    final Collection<CtxLevel> context,
                                    final boolean active,
                                    final String spName,
                                    final String spPassword,
                                    final String changeComment )
          throws ConfigException
    {
        if ( Utils.anyNull( user, repository, fileName ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.hasWriteAccess( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        String cleanPath = !Utils.isBlank( path ) && path.startsWith( "/" ) ? path.substring( 1 ) : path;
        String absPath = Utils.isBlank( cleanPath ) ? fileName : cleanPath + "/" + fileName;

        AbsoluteFilePath absoluteFilePath = getAbsFilePath( repository, absPath, null );
        if ( null == absoluteFilePath )
        {
            absoluteFilePath = new AbsoluteFilePath( repository, cleanPath, fileName );
            saveOrUpdateAudited( user, repository, absoluteFilePath, changeComment );
        }

        RepoFile file = new RepoFile( repository, absoluteFilePath, content, (Set<CtxLevel>) context );
        file.setActive( active );

        if ( !Utils.isBlank( spName ) )
        {
            SecurityProfile sp = getSecurityProfile( user, repository, null, spName );
            if ( null == sp )
            {
                throw new ConfigException( Error.Code.MISSING_SECURITY_PROFILE );
            }

            if ( !sp.isSecretValid( spPassword ) )
            {
                throw new ConfigException( Error.Code.INVALID_PASSWORD );
            }

            sp.sk = spPassword;
            file.setSecurityProfile( sp, spPassword );
        }

        file.setKeys( getKeysForContent( repository, user, FileUtils.getKeys( content ) ) );

        validateWriteAccess( repository, user, file );
        saveOrUpdateAudited( user, repository, file, changeComment );
        return file;
    }


    public RepoFile createRepoFile( final String apiIdentifier,
                                    final Repository repository,
                                    final Token token,
                                    final String path,
                                    final String fileName,
                                    final String content,
                                    final Collection<CtxLevel> context,
                                    final boolean active,
                                    final String spName,
                                    final String spPassword,
                                    final String changeComment )
          throws ConfigException
    {
        if ( Utils.anyNull( repository, fileName ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        String cleanPath = !Utils.isBlank( path ) && path.startsWith( "/" ) ? path.substring( 1 ) : path;
        String absPath = Utils.isBlank( cleanPath ) ? fileName : cleanPath + "/" + fileName;

        AbsoluteFilePath absoluteFilePath = getAbsFilePath( repository, absPath, null );
        if ( null == absoluteFilePath )
        {
            absoluteFilePath = new AbsoluteFilePath( repository, cleanPath, fileName );
            saveOrUpdateAuditedViaAPI( apiIdentifier, repository, absoluteFilePath, changeComment );
        }

        RepoFile file = new RepoFile( repository, absoluteFilePath, content, (Set<CtxLevel>) context );
        file.setActive( active );

        if ( !Utils.isBlank( spName ) )
        {
            SecurityProfile sp = getSecurityProfile( repository, null, spName );
            if ( null == sp )
            {
                throw new ConfigException( Error.Code.MISSING_SECURITY_PROFILE );
            }

            file.setSecurityProfile( sp, spPassword );
        }

        file.setKeys( getKeysForContent( repository, apiIdentifier, FileUtils.getKeys( content ), changeComment ) );

        validateWriteAccess( repository, token, file );
        saveOrUpdateAuditedViaAPI( apiIdentifier, repository, file, changeComment );
        return file;
    }


    private Set<PropertyKey> getKeysForContent( final Repository repository,
                                                final String apiIdentifier,
                                                final Collection<String> keyStrings,
                                                final String changeComment )
    {
        Set<PropertyKey> keys = new HashSet<>();

        if ( null != keyStrings )
        {
            keyStrings.stream().forEach( key -> {
                PropertyKey k = getKey( repository, key );
                if ( null != k )
                {
                    keys.add( k );
                }
                else
                {
                    k = new PropertyKey( repository, key );
                    keys.add( k );

                    saveOrUpdateAuditedViaAPI( apiIdentifier, repository, k, changeComment );
                }
            } );
        }

        return keys;
    }


    private Set<PropertyKey> getKeysForContent( final Repository repository,
                                                final UserAccount user,
                                                final Collection<String> keyStrings )
    {
        Set<PropertyKey> keys = new HashSet<>();

        if ( null != keyStrings )
        {
            keyStrings.stream().forEach( key -> {
                PropertyKey k = getKey( repository, key );
                if ( null != k )
                {
                    keys.add( k );
                }
                else
                {
                    k = new PropertyKey( repository, key );
                    keys.add( k );

                    saveOrUpdateAudited( user, repository, k );
                }
            } );
        }

        return keys;
    }


    public RepoFile updateRepoFile( final UserAccount user,
                                    final Repository repository,
                                    final Long fileId,
                                    final String path,
                                    final String filename,
                                    final boolean renameAll,
                                    final boolean updateRefs,
                                    final String content,
                                    final Collection<CtxLevel> context,
                                    final boolean active,
                                    final String spName,
                                    final String spPassword,
                                    final String currentPassword,
                                    final String changeComment )
    {
        if ( Utils.anyNull( user, repository ) || Utils.isBlank( filename ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.hasWriteAccess( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        return updateRepoFile( user,
                               null,
                               repository,
                               null,
                               fileId,
                               path,
                               filename,
                               renameAll,
                               updateRefs,
                               content,
                               context,
                               active,
                               spName,
                               spPassword,
                               currentPassword,
                               changeComment );
    }


    public RepoFile updateRepoFile( final String appIdentity,
                                    final Repository repository,
                                    final Token token,
                                    final Long fileId,
                                    final String path,
                                    final String filename,
                                    final String content,
                                    final Collection<CtxLevel> context,
                                    final boolean active,
                                    final String spName,
                                    final String newProfilePassword,
                                    final String currentPassword,
                                    final String changeComment )
    {
        if ( Utils.anyNull( repository ) || Utils.isBlank( filename ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        return updateRepoFile( null,
                               appIdentity,
                               repository,
                               token,
                               fileId,
                               path,
                               filename,
                               false,
                               false,
                               content,
                               context,
                               active,
                               spName,
                               newProfilePassword,
                               currentPassword,
                               changeComment );
    }


    private RepoFile updateRepoFile( final UserAccount user,
                                     final String appIdentity,
                                     final Repository repository,
                                     final Token token,
                                     final Long fileId,
                                     final String path,
                                     final String filename,
                                     final boolean renameAll,
                                     final boolean updateRefs,
                                     final String content,
                                     final Collection<CtxLevel> context,
                                     final boolean active,
                                     final String spName,
                                     final String newProfilePassword,
                                     final String currentPassword,
                                     final String changeComment )
          throws ConfigException
    {
        RepoFile file = get( RepoFile.class, fileId );
        if ( null == file || !file.getRepository().equals( repository ) )
        {
            throw new ConfigException( Error.Code.FILE_NOT_FOUND );
        }

        if ( null == user )
        {
            validateWriteAccess( repository, token, file );
        }

        // If a file is being renamed, there are things to do.
        AbsoluteFilePath originalAbsoluteFilePath = file.getAbsFilePath();
        if ( null == originalAbsoluteFilePath.getPath() || !originalAbsoluteFilePath.getPath()
                                                                                    .equalsIgnoreCase( path ) ||
             !originalAbsoluteFilePath
                   .getFilename()
                   .equalsIgnoreCase(
                         filename ) )
        {
            String absPath = Utils.isBlank( path ) ? filename : path + "/" + filename;
            AbsoluteFilePath absoluteFilePath = getAbsFilePath( repository, absPath, null );
            if ( null == absoluteFilePath )
            {
                absoluteFilePath = new AbsoluteFilePath( repository, path, filename );
                if ( null == user )
                {
                    saveOrUpdateAuditedViaAPI( appIdentity, repository, absoluteFilePath, changeComment );
                }
                else
                {
                    saveOrUpdateAudited( user, repository, absoluteFilePath, changeComment );
                }
            }

            file.setAbsFilePath( absoluteFilePath );

            if ( originalAbsoluteFilePath.getFiles().size() > 0 )
            {
                if ( renameAll )
                {
                    Iterator<RepoFile> fileIterator = originalAbsoluteFilePath.getFiles().iterator();
                    while ( fileIterator.hasNext() )
                    {
                        RepoFile f = fileIterator.next();
                        fileIterator.remove();
                        f.setAbsFilePath( absoluteFilePath );

                        if ( null == user )
                        {
                            saveOrUpdateAuditedViaAPI( appIdentity, repository, f, changeComment );
                        }
                        else
                        {
                            saveOrUpdateAudited( user, repository, f, changeComment );
                        }
                    }
                }
            }

            if ( originalAbsoluteFilePath.getFiles().size() == 0 || renameAll || updateRefs )
            {
                Iterator<Property> props = originalAbsoluteFilePath.getProperties().iterator();
                while ( props.hasNext() )
                {
                    Property property = props.next();
                    props.remove();
                    property.setAbsoluteFilePath( absoluteFilePath );

                    if ( null == user )
                    {
                        saveOrUpdateAuditedViaAPI( appIdentity, repository, property, changeComment );
                    }
                    else
                    {
                        saveOrUpdateAudited( user, repository, property, changeComment );
                    }
                }
            }

            if ( originalAbsoluteFilePath.getFiles().size() == 0 && originalAbsoluteFilePath.getProperties().size() == 0 )
            {
                deleteAudited( user, repository, originalAbsoluteFilePath, changeComment );
            }
        }

        file.setContext( context );
        file.setActive( active );
        file.setContent( content, currentPassword );

        if ( !Utils.isBlank( spName ) )
        {
            SecurityProfile sp = getSecurityProfile( repository, null, spName );
            if ( null == sp )
            {
                throw new ConfigException( Error.Code.MISSING_SECURITY_PROFILE );
            }

            if ( !sp.isSecretValid( newProfilePassword ) )
            {
                throw new ConfigException( Error.Code.INVALID_PASSWORD );
            }

            sp.sk = newProfilePassword;
            file.setSecurityProfile( sp, currentPassword );
        }
        else
        {
            file.removeSecurityProfile( currentPassword );
        }

        file.setKeys( getKeysForContent( repository, user, FileUtils.getKeys( content ) ) );

        if ( null == user )
        {
            saveOrUpdateAuditedViaAPI( appIdentity, repository, file, changeComment );
        }
        else
        {
            validateWriteAccess( repository, user, file );
            saveOrUpdateAudited( user, repository, file, changeComment );
        }

        return file;
    }


    public AbsoluteFilePath getAbsFilePath( final Repository repository,
                                            final String absPath,
                                            final Date date )
    {
        if ( null == repository || Utils.isBlank( absPath ) )
        {
            return null;
        }

        try
        {
            if ( null == date )
            {
                return (AbsoluteFilePath) em.createNamedQuery( "AbsFilePath.getByAbsPath" )
                                            .setLockMode( LockModeType.NONE )
                                            .setParameter( "absPath", absPath )
                                            .setParameter( "repository", repository )
                                            .getSingleResult();
            }

            AuditReader reader = AuditReaderFactory.get( em );
            Number rev = reader.getRevisionNumberForDate( date );

            AuditQuery kq = reader.createQuery().forEntitiesAtRevision( AbsoluteFilePath.class, rev );
            kq.add( AuditEntity.property( "repository" ).eq( repository ) );
            kq.add( AuditEntity.property( "absPath" ).eq( absPath ) );

            return (AbsoluteFilePath) kq.getSingleResult();
        }
        catch ( NoResultException e )
        {
            return null;
        }
        catch ( Exception e )
        {
            handleException( e );
            return null;
        }
    }


    public void deleteRepoFile( final UserAccount user,
                                final Repository repository,
                                final Long fileId )
          throws ConfigException
    {
        if ( Utils.anyNull( user, repository, fileId ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.hasWriteAccess( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        RepoFile file = get( RepoFile.class, fileId );
        if ( null == file || !file.getRepository().equals( repository ) )
        {
            throw new ConfigException( Error.Code.FILE_NOT_FOUND );
        }

        AbsoluteFilePath absoluteFilePath = file.getAbsFilePath();
        absoluteFilePath.removeFile( file );

        validateWriteAccess( repository, user, file );
        deleteAudited( user, repository, file );
        if ( absoluteFilePath.getFiles().size() == 0 && absoluteFilePath.getProperties().size() == 0 )
        {
            deleteAudited( user, repository, absoluteFilePath );
        }
    }


    public void deleteRepoFile( final String appIdentity,
                                final Repository repository,
                                final Token token,
                                final Long fileId,
                                final String changeComment )
          throws ConfigException
    {
        RepoFile file = get( RepoFile.class, fileId );
        if ( null == file || !file.getRepository().equals( repository ) )
        {
            return;
        }

        validateWriteAccess( repository, token, file );

        AbsoluteFilePath absoluteFilePath = file.getAbsFilePath();
        absoluteFilePath.removeFile( file );

        deleteAuditedViaAPI( appIdentity, repository, file, changeComment );

        if ( absoluteFilePath.getFiles().size() == 0 && absoluteFilePath.getProperties().size() == 0 )
        {
            deleteAuditedViaAPI( appIdentity, repository, absoluteFilePath, changeComment );
        }
    }


    public RepoFile getRepoFile( final UserAccount user,
                                 final Repository repository,
                                 final Long fileId,
                                 final Date time )
          throws ConfigException
    {
        if ( !repository.hasReadAccess( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        if ( null == time )
        {
            RepoFile file = get( RepoFile.class, fileId );
            if ( file.getRepository().equals( repository ) )
            {
                return file;
            }
            else
            {
                throw new ConfigException( Error.Code.FILE_NOT_FOUND );
            }
        }

        AuditReader reader = AuditReaderFactory.get( em );
        Number rev = reader.getRevisionNumberForDate( time );

        AuditQuery query = reader.createQuery().forEntitiesAtRevision( RepoFile.class, rev );
        query.add( AuditEntity.property( "repository" ).eq( repository ) );
        query.add( AuditEntity.id().eq( fileId ) );

        try
        {
            return (RepoFile) query.getSingleResult();
        }
        catch ( NoResultException e )
        {
            return null;
        }
        catch ( Exception e )
        {
            handleException( e );
            return null;
        }
    }


    public RepoFile getRepoFile( final Repository repository,
                                 final String absPath,
                                 final Set<CtxLevel> context,
                                 final Date date )
          throws ConfigException
    {
        AbsoluteFilePath absFile = getAbsFilePath( repository, absPath, date );
        if ( null == absFile )
        {
            return null;
        }

        return absFile.getFileForContext( context );
    }


    public void renameDirectory( final Repository repository,
                                 final UserAccount user,
                                 final String oldPath,
                                 final String newPath )
          throws ConfigException
    {
        if ( !repository.hasWriteAccess( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        if ( Utils.anyBlank( oldPath, newPath ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        try
        {
            List<AbsoluteFilePath> absoluteFilePaths = em.createNamedQuery( "AbsFilePath.searchByPath" )
                                                         .setLockMode( LockModeType.NONE )
                                                         .setParameter( "path", oldPath + "%" )
                                                         .setParameter( "repository", repository )
                                                         .getResultList();

            if ( null == absoluteFilePaths || absoluteFilePaths.size() == 0 )
            {
                return;
            }

            absoluteFilePaths.stream().forEach( a -> {

                String updatedPath = a.getPath().replace( oldPath, newPath );
                a.setPath( updatedPath );

                saveOrUpdateAudited( user, repository, a );
            } );
        }
        catch ( NoResultException e )
        {
        }
        catch ( Exception e )
        {
            handleException( e );
        }
    }


    public void deleteDirectory( final Repository repository,
                                 final UserAccount user,
                                 final String path )
          throws ConfigException
    {
        if ( Utils.anyBlank( path ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.hasWriteAccess( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        String cleanPath = path.startsWith( "/" ) ? path.substring( 1 ) : path;
        try
        {
            List<AbsoluteFilePath> absoluteFilePaths = em.createNamedQuery( "AbsFilePath.searchByPath" )
                                                         .setLockMode( LockModeType.NONE )
                                                         .setParameter( "path", cleanPath + "%" )
                                                         .setParameter( "repository", repository )
                                                         .getResultList();

            if ( null == absoluteFilePaths || absoluteFilePaths.size() == 0 )
            {
                return;
            }

            absoluteFilePaths.stream().forEach( a -> {
                if ( null != a.getProperties() && a.getProperties().size() > 0 )
                {
                    throw new ConfigException( Error.Code.FILE_REFERENCED_BY_VALUE );
                }

                deleteAudited( user, repository, a );
            } );
        }
        catch ( NoResultException e )
        {
        }
        catch ( Exception e )
        {
            handleException( e );
        }
    }


    public Collection<RepoFile> getRepoFilesForAPI( final Repository repository,
                                                    final Date date )
          throws ConfigException
    {
        if ( null == repository )
        {
            throw new ConfigException( Error.Code.REPOSITORY_NOT_FOUND );
        }

        if ( null == date )
        {
            return repository.getFiles();
        }

        try
        {
            AuditReader reader = AuditReaderFactory.get( em );
            Number rev = reader.getRevisionNumberForDate( date );

            AuditQuery query = reader.createQuery().forEntitiesAtRevision( RepoFile.class, rev ).add( AuditEntity.property(
                  "repository" ).eq( repository ) );

            return query.getResultList();
        }
        catch ( NoResultException e )
        {
            return null;
        }
        catch ( Exception e )
        {
            handleException( e );
            return null;
        }
    }


    public Collection<RepoFile> getRepoFiles( final Repository repository,
                                              final UserAccount user,
                                              final String searchTerm,
                                              final Date date )
          throws ConfigException
    {
        if ( Utils.anyNull( repository ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        //        if (!repository.isDemo() && null == user)
        //            throw new ConfigException(Error.Code.MISSING_PARAMS);
        //
        if ( !repository.isDemo() && !repository.hasReadAccess( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        if ( null == date )
        {
            if ( Utils.isBlank( searchTerm ) )
            {
                return repository.getFiles();
            }
            else
            {
                try
                {
                    return em.createNamedQuery( "RepoFile.search" )
                             .setLockMode( LockModeType.NONE )
                             .setParameter( "repository", repository )
                             .setParameter( "searchTerm", "%" + searchTerm + "%" )
                             .getResultList();
                }
                catch ( NoResultException e )
                {
                    return null;
                }
                catch ( Exception e )
                {
                    handleException( e );
                    return null;
                }
            }
        }
        else
        {
            try
            {
                AuditReader reader = AuditReaderFactory.get( em );
                Number rev = reader.getRevisionNumberForDate( date );

                AuditQuery query = reader.createQuery()
                                         .forEntitiesAtRevision( RepoFile.class, rev )
                                         .add( AuditEntity.property( "repository" ).eq( repository ) );

                if ( !Utils.isBlank( searchTerm ) )
                {
                    query.add( AuditEntity.property( "content" ).like( "%" + searchTerm + "%" ) );
                }

                return query.getResultList();
            }
            catch ( NoResultException e )
            {
                return null;
            }
            catch ( Exception e )
            {
                handleException( e );
                return null;
            }
        }
    }


    public RepoFile getAuditConfigFile( final UserAccount user,
                                        final Repository repository,
                                        final Long fileId,
                                        final Long revId )
          throws ConfigException
    {
        try
        {
            if ( !repository.hasReadAccess( user ) )
            {
                throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
            }

            AuditReader reader = AuditReaderFactory.get( em );

            AuditQuery query = reader.createQuery()
                                     .forRevisionsOfEntity( RepoFile.class, true, true )
                                     .add( AuditEntity.revisionNumber().ge( revId ) )
                                     .add( AuditEntity.revisionNumber().le( revId ) )
                                     .add( AuditEntity.property( "repository" ).eq( repository ) )
                                     .add( AuditEntity.id().eq( fileId ) );

            return (RepoFile) query.getSingleResult();
        }
        catch ( NoResultException e )
        {
            return null;
        }
        catch ( Exception e )
        {
            handleException( e );
            return null;
        }
    }


    public List<PropertyKey> getKeys( final UserAccount user,
                                      final Repository repository,
                                      final Collection<String> keys,
                                      final Date time )
          throws ConfigException
    {
        if ( Utils.anyNull( repository, keys ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !repository.hasReadAccess( user ) )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        if ( null == time )
        {
            Collection<String> upperKeys = new ArrayList<>();
            keys.forEach( k -> upperKeys.add( k.toUpperCase() ) );

            try
            {
                return em.createNamedQuery( "Key.getKeys" )
                         .setLockMode( LockModeType.NONE )
                         .setParameter( "repository", repository )
                         .setParameter( "keys", upperKeys )
                         .getResultList();
            }
            catch ( NoResultException e )
            {
                return Collections.EMPTY_LIST;
            }
            catch ( Exception e )
            {
                handleException( e );
                return Collections.EMPTY_LIST;
            }
        }

        AuditReader reader = AuditReaderFactory.get( em );
        Number rev = reader.getRevisionNumberForDate( time );

        AuditQuery query = reader.createQuery().forEntitiesAtRevision( PropertyKey.class, rev );
        query.add( AuditEntity.property( "repository" ).eq( repository ) );
        query.add( AuditEntity.property( "key" ).in( keys ) );

        try
        {
            return query.getResultList();
        }
        catch ( NoResultException e )
        {
            return null;
        }
        catch ( Exception e )
        {
            handleException( e );
            return null;
        }
    }


    // ------------------------------------------------------------------------------------
    // System administrators
    // ------------------------------------------------------------------------------------
    public void addSystemAdmin( final UserAccount user )
          throws ConfigException
    {
        if ( Utils.anyNull( user ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( CollectionUtils.isEmpty( getSystemAdmins() ) )
        {
            user.setConfigHubAdmin( true );
            saveOrUpdateNonAudited( user );
        }
    }


    public void addSystemAdmin( final UserAccount user,
                                final UserAccount newSystemAdmin )
          throws ConfigException
    {
        if ( Utils.anyNull( user, newSystemAdmin ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !user.isConfigHubAdmin() )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        newSystemAdmin.setConfigHubAdmin( true );
        saveOrUpdateNonAudited( newSystemAdmin );
    }


    public void removeSystemAdmin( final UserAccount user,
                                   final UserAccount newSystemAdmin )
          throws ConfigException
    {
        if ( Utils.anyNull( user, newSystemAdmin ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !user.isConfigHubAdmin() )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        List<UserAccount> admins = getSystemAdmins();
        if ( null == admins || admins.size() < 2 )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        newSystemAdmin.setConfigHubAdmin( false );
        saveOrUpdateNonAudited( newSystemAdmin );
    }


    public List<UserAccount> getSystemAdmins()
          throws ConfigException
    {
        try
        {
            return em.createNamedQuery( "Users.sysAdmins" )
                     .setLockMode( LockModeType.NONE )
                     .getResultList();
        }
        catch ( NoResultException e )
        {
            return Collections.EMPTY_LIST;
        }
        catch ( Exception e )
        {
            handleException( e );
            return Collections.EMPTY_LIST;
        }
    }


    // ------------------------------------------------------------------------------------
    // ConfigHub Configuration
    // ------------------------------------------------------------------------------------
    public void save( final UserAccount user,
                      final LdapConfig ldapConfig )
    {
        if ( Utils.anyNull( user, ldapConfig ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !user.isConfigHubAdmin() )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        saveOrUpdateNonAudited( getLdapCfg( "ldapEnabled", String.valueOf( ldapConfig.isLdapEnabled() ) ) );
        saveOrUpdateNonAudited( getLdapCfg( "localAccountsEnabled", String.valueOf( ldapConfig.isLocalAccountsEnabled() ) ) );
        saveOrUpdateNonAudited( getLdapCfg( "systemUsername", ldapConfig.getSystemUsername() ) );
        saveOrUpdateNonAudited( getLdapCfg( "systemPassword", ldapConfig.getSystemPassword() ) );
        saveOrUpdateNonAudited( getLdapCfg( "ldapUrl", ldapConfig.getLdapUrl() ) );
        saveOrUpdateNonAudited( getLdapCfg( "trustAllCertificates", String.valueOf( ldapConfig.isTrustAllCertificates() ) ) );
        saveOrUpdateNonAudited( getLdapCfg( "activeDirectory", String.valueOf( ldapConfig.isActiveDirectory() ) ) );
        saveOrUpdateNonAudited( getLdapCfg( "searchBase", ldapConfig.getSearchBase() ) );
        saveOrUpdateNonAudited( getLdapCfg( "searchPattern", ldapConfig.getSearchPattern() ) );
        saveOrUpdateNonAudited( getLdapCfg( "nameAttribute", ldapConfig.getNameAttribute() ) );
        saveOrUpdateNonAudited( getLdapCfg( "emailAttribute", ldapConfig.getEmailAttribute() ) );
        saveOrUpdateNonAudited( getLdapCfg( "groupSearchBase", ldapConfig.getGroupSearchBase() ) );
        saveOrUpdateNonAudited( getLdapCfg( "groupIdAttribute", ldapConfig.getGroupIdAttribute() ) );
        saveOrUpdateNonAudited( getLdapCfg( "groupSearchPattern", ldapConfig.getGroupSearchPattern() ) );

        Auth.updateLdap( ldapConfig );
    }


    private SystemConfig getLdapCfg( final String key,
                                     final String value )
    {
        SystemConfig config = getSystemConfig( SystemConfig.ConfigGroup.LDAP, key );

        if ( null == config )
        {
            config = new SystemConfig();
        }

        config.setConfigGroup( SystemConfig.ConfigGroup.LDAP );
        config.setKey( key );
        config.setValue( value );

        return config;
    }


    public void saveSystemConfig( final UserAccount user,
                                  final SystemConfig.ConfigGroup group,
                                  final String key,
                                  final String value )
          throws ConfigException
    {
        if ( Utils.anyNull( user, group, key ) )
        {
            throw new ConfigException( Error.Code.MISSING_PARAMS );
        }

        if ( !user.isConfigHubAdmin() )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }

        SystemConfig config = getSystemConfig( group, key );

        if ( null == config )
        {
            config = new SystemConfig();
        }

        config.setConfigGroup( group );
        config.setKey( key );
        config.setValue( value );

        saveOrUpdateNonAudited( config );
    }


    public Map<String, SystemConfig> getSystemConfig( final SystemConfig.ConfigGroup group )
          throws ConfigException
    {
        try
        {
            List<SystemConfig> list = em.createNamedQuery( "SysConfig.byGroup" )
                                        .setLockMode( LockModeType.NONE )
                                        .setParameter( "groupName", group )
                                        .getResultList();

            Map<String, SystemConfig> map = new HashMap<>();
            list.forEach( e -> map.put( e.getKey(), e ) );

            return map;
        }
        catch ( NoResultException e )
        {
            return Collections.EMPTY_MAP;
        }
        catch ( Exception e )
        {
            handleException( e );
            return Collections.EMPTY_MAP;
        }
    }


    public SystemConfig getSystemConfig( final SystemConfig.ConfigGroup group,
                                         final String key )
          throws ConfigException
    {
        try
        {
            return (SystemConfig) em.createNamedQuery( "SysConfig.byKey" )
                                    .setLockMode( LockModeType.NONE )
                                    .setParameter( "groupName", group )
                                    .setParameter( "key", key )
                                    .getSingleResult();
        }
        catch ( NoResultException e )
        {
            return null;
        }
        catch ( Exception e )
        {
            handleException( e );
            return null;
        }
    }
}

