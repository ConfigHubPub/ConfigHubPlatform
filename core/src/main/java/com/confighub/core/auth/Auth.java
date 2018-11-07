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

package com.confighub.core.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.repository.Repository;
import com.confighub.core.security.CipherTransformation;
import com.confighub.core.store.Store;
import com.confighub.core.system.conf.LdapConfig;
import com.confighub.core.user.UserAccount;
import com.confighub.core.utils.Utils;
import org.apache.commons.io.IOUtils;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public final class Auth
{
    private static final Logger log = LogManager.getLogger( Auth.class );

    private static String userSecret;

    private static String apiTokenSecret;

    private static String passwordResetSecret;

    private static String securityGroupPassword;

    private static JWTVerifier passwordVerifier;

    private static JWTVerifier apiTokenVerifier;

    private static JWTVerifier userTokenVerifier;

    private static final String issuer = "ConfigHub";

    private static boolean ldapEnabled = false;

    private static boolean localAccountsEnabled = true;

    private static LdapConfig ldapConfig;

    private static LdapNetworkConnection ldapNetworkConnection;

    private static final LdapConnector ldapConnector = new LdapConnector();

    static
    {
        init();
    }

    public static void init()
    {
        log.warn( "Initializing Auth keys..." );

        // Used to generate user token - for logging into the UI
        userSecret = readLocalFile( "userAuth.key" );

        // Used to create a repository-token - for client use via API
        apiTokenSecret = readLocalFile( "apiToken.key" );

        // If a user forgets the password - this token is used to send the email back to the user
        // with reset instructions
        passwordResetSecret = readLocalFile( "userAuthReset.key" );

        // Used to encrypt security group passwords when persisting them to the database.
        securityGroupPassword = readLocalFile( "securityGroup.key" );

        try
        {
            passwordVerifier = JWT.require( Algorithm.HMAC256( passwordResetSecret ) ).withIssuer( issuer ).build();
            apiTokenVerifier = JWT.require( Algorithm.HMAC256( apiTokenSecret ) ).withIssuer( issuer ).build();
            userTokenVerifier = JWT.require( Algorithm.HMAC256( userSecret ) ).withIssuer( issuer ).build();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            System.exit( 1 );
        }
        log.warn( "Initializing Auth keys completed" );
    }


    public static void updateLdap( final LdapConfig config )
    {
        ldapConfig = config;

        try
        {
            ldapEnabled = ldapConfig.isLdapEnabled();

            if ( ldapEnabled )
            {
                ldapNetworkConnection = ldapConnector.connect( ldapConfig.toConnectionConfig() );
            }
            else
            {
                ldapNetworkConnection = null;
            }

            localAccountsEnabled = ldapEnabled ? ldapConfig.isLocalAccountsEnabled() : true;
        }
        catch ( LdapException e )
        {
            ldapEnabled = false;
            localAccountsEnabled = true;
            log.error( "Failed to connect to LDAP: " + e.getMessage() );
        }
    }


    public static UserAccount ldapAuth( final String username,
                                        final String password,
                                        final boolean createUserIfNotStored,
                                        final Store store )
          throws ConfigException
    {
        try
        {
            final LdapEntry entry = ldapConnector.search( ldapNetworkConnection,
                                                          ldapConfig,
                                                          username );
            if ( null == entry )
            {
                log.warn( "Could not find user in LDAP.  Username: {}", username );
                throw new LdapException();
            }

            boolean authenticated = ldapConnector.authenticate( ldapNetworkConnection,
                                                                entry.getBindPrincipal(),
                                                                password );
            if ( !authenticated )
            {
                throw new ConfigException( Error.Code.USER_AUTH );
            }

            // At this point, the user has authenticated via LDAP

            UserAccount ua = store.getUserAccount( username );

            if ( null == ua && createUserIfNotStored )
            {
                store.begin();
                final String emailAddress = entry.getAttributes().getOrDefault( ldapConfig.getEmailAttribute(), null );
                ua = store.createUser( emailAddress,
                                       username,
                                       null,
                                       UserAccount.AccountType.LDAP );
                ua.setName( entry.getAttributes().getOrDefault( ldapConfig.getNameAttribute(), username ) );
                store.commit();
            }

            return ua;
        }
        catch ( LdapException | RuntimeException e )
        {
            log.error( "Failed to auth username: " + username + " to LDAP" );
            return null;
        }
    }


    public static final CipherTransformation internalCipher =
          CipherTransformation.DESede_CBC_PKCS5Padding;


    private Auth()
    {
    }


    public static String createUserToken( final UserAccount user )
          throws ConfigException
    {
        if ( null == user )
        {
            return null;
        }

        try
        {
            return JWT.create()
                      .withClaim( "userId", user.getId().intValue() )
                      .withClaim( "username", user.getUsername() )
                      .withClaim( "email", user.getEmail() )
                      .withIssuer( issuer )
                      .withExpiresAt( new Date( System.currentTimeMillis() + 604800000 ) ) // one week
                      .sign( Algorithm.HMAC256( userSecret ) );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            log.error( "Failed to create user token.  Contact ConfigHub Support." );
            throw new ConfigException( Error.Code.INTERNAL_ERROR );
        }
    }


    public static Map<String, Object> validateUser( final String token )
    {
        if ( Utils.isBlank( token ) )
        {
            return null;
        }

        try
        {
            Map<String, Object> claims = new HashMap<>();

            DecodedJWT decoded = userTokenVerifier.verify( token );
            claims.put( "userId", decoded.getClaim( "userId" ).asInt() );

            return claims;
        }
        catch ( Exception e )
        {
            throw new ConfigException( Error.Code.USER_ACCESS_DENIED );
        }
    }


    /**
     * Password reset token is valid for 60 minutes.
     *
     * @param claims
     * @return
     */
    public static String getPassResetToken( final Map<String, Object> claims )
          throws ConfigException
    {
        if ( null == claims )
        {
            return null;
        }

        try
        {
            JWTCreator.Builder builder = JWT.create();
            claims.keySet().forEach( k -> builder.withClaim( k, claims.get( k ) ) );

            return builder.withIssuer( issuer )
                          .withExpiresAt( new Date( System.currentTimeMillis() + 3600000 ) )
                          .sign( Algorithm.HMAC256( passwordResetSecret ) );
        }
        catch ( Exception e )
        {
            log.error( "Failed to create user token.  Contact ConfigHub Support." );
            throw new ConfigException( Error.Code.INTERNAL_ERROR );
        }
    }


    public static Map<String, Object> verifyPasswordChangeToken( final String token )
          throws ConfigException
    {
        if ( Utils.isBlank( token ) )
        {
            return null;
        }

        try
        {
            Map<String, Object> claims = new HashMap<>();

            DecodedJWT decoded = passwordVerifier.verify( token );
            claims.put( "email", decoded.getClaim( "email" ).asString() );

            return claims;
        }
        catch ( Exception e )
        {
            log.warn( "Password reset token failed: " + e.getMessage() );
            throw new ConfigException( Error.Code.INVALID_PASS_CHANGE_TOKEN );
        }
    }


    public static Map<String, Object> validateApiToken( final String token )
          throws ConfigException
    {
        try
        {
            Map<String, Object> claims = new HashMap<>();

            DecodedJWT decoded = apiTokenVerifier.verify( token );
            claims.put( "rid", decoded.getClaim( "rid" ).asInt() );

            return claims;
        }
        catch ( Exception e )
        {
            throw new ConfigException( Error.Code.INVALID_CLIENT_TOKEN );
        }
    }


    public static String getApiToken( final Repository repository )
          throws ConfigException
    {
        if ( null == repository )
        {
            return null;
        }

        try
        {
            return JWT.create()
                      .withClaim( "rid", repository.getId().intValue() )
                      .withClaim( "ts", (int) System.currentTimeMillis() )
                      .withIssuer( issuer )
                      .sign( Algorithm.HMAC256( apiTokenSecret ) );
        }
        catch ( Exception e )
        {
            log.error( "Failed to create API token.  Contact ConfigHub Support." );

            throw new ConfigException( Error.Code.INTERNAL_ERROR );
        }
    }


    private static String readLocalFile( String fileName )
          throws ConfigException
    {
        try
        {
            ClassLoader classLoader = Auth.class.getClassLoader();
            File file = new File( classLoader.getResource( "auth/" + fileName ).getFile() );
            InputStream inputStream = new FileInputStream( file );

            if ( null != inputStream )
            {
                String s = IOUtils.toString( inputStream, "UTF-8" );
                if ( null != s )
                {
                    return s.replaceAll( "\n", "" );
                }
            }

            throw new ConfigException( "Cannot read file: " + file );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            throw new ConfigException( Error.Code.INTERNAL_ERROR );
        }
    }


    public static String getSecurityGroupPassword()
    {
        return securityGroupPassword;
    }


    public static boolean isLdapEnabled()
    {
        return ldapEnabled;
    }


    public static boolean isLocalAccountsEnabled()
    {
        return localAccountsEnabled;
    }
}
