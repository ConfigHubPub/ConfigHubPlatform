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

package com.confighub.api.repository.client.v1;

import com.confighub.api.repository.client.AClientAccessValidation;
import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.repository.AbsoluteFilePath;
import com.confighub.core.repository.CtxLevel;
import com.confighub.core.repository.Property;
import com.confighub.core.repository.PropertyKey;
import com.confighub.core.repository.RepoFile;
import com.confighub.core.security.SecurityProfile;
import com.confighub.core.security.Token;
import com.confighub.core.store.Store;
import com.confighub.core.store.diff.ADiffTracker;
import com.confighub.core.utils.ContextParser;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;


@Path( "/push" )
@Produces( "application/json" )
@Consumes( MediaType.APPLICATION_JSON )
public class APIPush
      extends AClientAccessValidation
{

    @POST
    @Path( "/{account}/{repository}" )
    public Response pushStructuredData( @PathParam( "account" ) String account,
                                        @PathParam( "repository" ) String repositoryName,
                                        @HeaderParam( "Client-Version" ) String version,
                                        @HeaderParam( "Application-Name" ) String appName,
                                        @HeaderParam( "Security-Profile-Auth" ) String securityProfiles,
                                        @HeaderParam( "X-Forwarded-For" ) String remoteIp,
                                        String postJson )
    {
        Store store = new Store();
        Gson gson = new GsonBuilder().serializeNulls().create();

        try
        {
            ADiffTracker.track();
            getRepositoryFromUrl( account, repositoryName, store, false );
            validatePush( null, version, appName, remoteIp, store );
            pushData( postJson, appName, gson, store, false, null );
        }
        catch ( ConfigException e )
        {
            store.rollback();
            return Response.notModified( e.getErrorCode().getMessage() + ( null == e.getJson() ? "" : "; " + e.getJson() ) )
                           .build();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            store.rollback();
            return Response.notModified( Error.Code.INVALID_JSON_FORMAT.getMessage() ).build();
        }
        finally
        {
            store.close();
        }

        return Response.ok().build();
    }


    @POST
    public Response pushStructuredData( @HeaderParam( "Client-Token" ) String clientToken,
                                        @HeaderParam( "Client-Version" ) String version,
                                        @HeaderParam( "Application-Name" ) String appName,
                                        @HeaderParam( "Security-Profile-Auth" ) String securityProfiles,
                                        @HeaderParam( "X-Forwarded-For" ) String remoteIp,
                                        String postJson )
    {
        Store store = new Store();
        Gson gson = new GsonBuilder().serializeNulls().create();

        try
        {
            ADiffTracker.track();
            getRepositoryFromToken( clientToken, store );
            Token token = repository.getToken( clientToken );

            if ( null == token )
            {
                return Response.notModified( Error.Code.INVALID_CLIENT_TOKEN.getMessage() ).build();
            }

            validatePush( clientToken, version, appName, remoteIp, store );
            pushData( postJson, appName, gson, store, token.isForceKeyPushEnabled(), token );
        }
        catch ( ConfigException e )
        {
            store.rollback();
            return Response.notModified( e.getErrorCode().getMessage() + ( null == e.getJson() ? "" : "; " + e.getJson() ) )
                           .build();
        }
        catch ( Exception e )
        {
            store.rollback();
            return Response.notModified( Error.Code.INVALID_JSON_FORMAT.getMessage() ).build();
        }
        finally
        {
            store.close();
        }

        return Response.ok().build();
    }


    private void pushData( final String postJson,
                           final String appName,
                           final Gson gson,
                           final Store store,
                           final boolean forcePushEnabled,
                           final Token token )
          throws ConfigException
    {
        JsonObject jsonObject = gson.fromJson( postJson, JsonObject.class );

        String changeComment = jsonObject.has( "changeComment" )
                               ? jsonObject.get( "changeComment" ).getAsString()
                               : null;

        boolean enableKeyCreation = jsonObject.has( "enableKeyCreation" )
                                    ? jsonObject.get( "enableKeyCreation" ).getAsBoolean()
                                    : false || forcePushEnabled;


        JsonArray arr = jsonObject.getAsJsonArray( "data" );
        store.begin();

        for ( int i = 0; i < arr.size(); i++ )
        {
            JsonObject entry = gson.fromJson( arr.get( i ), JsonObject.class );

            //////////////////////////////////////////////////////////////
            // Parse file entry
            //////////////////////////////////////////////////////////////
            if ( entry.has( "file" ) )
            {
                try
                {
                    String absPath = entry.get( "file" ).getAsString();
                    String content = entry.has( "content" ) ? entry.get( "content" ).getAsString() : "";
                    boolean active = entry.has( "active" ) ? entry.get( "active" ).getAsBoolean() : true;
                    String spName = entry.has( "securityGroup" ) ? entry.get( "securityGroup" ).getAsString() : null;
                    String spPassword = entry.has( "password" ) ? entry.get( "password" ).getAsString() : null;

                    int li = absPath.lastIndexOf( "/" );

                    String path = li > 0 ? absPath.substring( 0, li ) : "";
                    String fileName = li > 0 ? absPath.substring( li + 1, absPath.length() ) : absPath;

                    String contextString = entry.get( "context" ).getAsString();
                    Set<CtxLevel> context = ContextParser.parseAndCreateViaApi( contextString,
                                                                                repository,
                                                                                store,
                                                                                appName,
                                                                                changeComment );

                    RepoFile file = store.getRepoFile( repository, absPath, context, null );

                    boolean isDelete = entry.has( "opp" )
                                       ? "delete".equalsIgnoreCase( entry.get( "opp" ).getAsString() )
                                       : false;

                    if ( null == file )
                    {
                        if ( isDelete )
                        {
                            continue;
                        }

                        store.createRepoFile( appName,
                                              repository,
                                              token,
                                              path,
                                              fileName,
                                              content,
                                              context,
                                              active,
                                              spName,
                                              spPassword,
                                              changeComment );
                    }
                    else
                    {
                        if ( isDelete )
                        {
                            store.deleteRepoFile( appName, repository, token, file.getId(), changeComment );
                        }
                        else
                        {
                            store.updateRepoFile( appName,
                                                  repository,
                                                  token,
                                                  file.getId(),
                                                  path,
                                                  fileName,
                                                  content,
                                                  context,
                                                  active,
                                                  spName,
                                                  spPassword,
                                                  spPassword,
                                                  changeComment );
                        }
                    }
                }
                catch ( ConfigException e )
                {
                    throw new ConfigException( e.getErrorCode(), entry );
                }
            }
        }

        for ( int i = 0; i < arr.size(); i++ )
        {
            JsonObject entry = gson.fromJson( arr.get( i ), JsonObject.class );

            //////////////////////////////////////////////////////////////
            // Parse key entry
            //////////////////////////////////////////////////////////////
            if ( entry.has( "key" ) )
            {
                String key = entry.get( "key" ).getAsString();
                PropertyKey.ValueDataType valueDataType = PropertyKey.ValueDataType.Text;
                try
                {
                    valueDataType = entry.has( "vdt" )
                                    ? PropertyKey.ValueDataType.valueOf( entry.get( "vdt" ).getAsString() )
                                    : PropertyKey.ValueDataType.Text;
                }
                catch ( Exception ignore )
                {
                }

                boolean isDeleteKey = entry.has( "opp" )
                                      ? "delete".equalsIgnoreCase( entry.get( "opp" ).getAsString() )
                                      : false;

                PropertyKey propertyKey = store.getKey( repository, key );
                if ( null == propertyKey )
                {
                    if ( isDeleteKey )
                    {
                        continue;
                    }

                    if ( !enableKeyCreation )
                    {
                        throw new ConfigException( Error.Code.KEY_CREATION_VIA_API_DISABLED, entry );
                    }

                    propertyKey = new PropertyKey( repository, key, valueDataType );
                }
                else if ( !propertyKey.isPushValueEnabled() && !forcePushEnabled )
                {
                    JsonObject ej = new JsonObject();
                    ej.addProperty( "key", key );
                    ej.addProperty( "push", false );
                    throw new ConfigException( Error.Code.PUSH_DISABLED, ej );
                }
                else if ( repository.isValueTypeEnabled() )
                {
                    propertyKey.setValueDataType( valueDataType );
                }

                if ( entry.has( "securityGroup" ) && entry.has( "password" ) )
                {
                    String spName = entry.get( "securityGroup" ).getAsString();
                    String password = entry.get( "password" ).getAsString();

                    passwords.put( spName, password );

                    if ( !Utils.anyBlank( spName, password ) )
                    {
                        SecurityProfile sp = store.getSecurityProfile( repository, null, spName );
                        if ( null == sp )
                        {
                            JsonObject ej = new JsonObject();
                            ej.addProperty( "securityGroup", spName );
                            throw new ConfigException( Error.Code.MISSING_SECURITY_PROFILE, ej );
                        }

                        propertyKey.setSecurityProfile( sp, password );
                    }
                }

                if ( isDeleteKey )
                {
                    String pass = propertyKey.isSecure()
                                  ? passwords.get( propertyKey.getSecurityProfile().getName() )
                                  : null;

                    store.deleteKeyAndProperties( appName, repository, token, key, pass, changeComment );
                    continue;
                }

                if ( entry.has( "readme" ) && !entry.get( "readme" ).isJsonNull() )
                {
                    propertyKey.setReadme( entry.get( "readme" ).getAsString() );
                }

                if ( entry.has( "deprecated" ) )
                {
                    propertyKey.setDeprecated( entry.get( "deprecated" ).getAsBoolean() );
                }

                if ( entry.has( "push" ) )
                {
                    propertyKey.setPushValueEnabled( entry.get( "push" ).getAsBoolean() );
                }

                if ( entry.has( "values" ) )
                {
                    JsonArray values = gson.fromJson( entry.get( "values" ), JsonArray.class );

                    for ( int j = 0; j < values.size(); j++ )
                    {
                        JsonObject valueJson = gson.fromJson( values.get( j ), JsonObject.class );

                        if ( !valueJson.has( "context" ) )
                        {
                            JsonObject ej = new JsonObject();
                            ej.addProperty( "key", key );
                            ej.addProperty( "push", false );

                            throw new ConfigException( Error.Code.CONTEXT_NOT_SPECIFIED, ej );
                        }

                        try
                        {
                            String context = valueJson.get( "context" ).getAsString();
                            Set<CtxLevel> ctxLevels = ContextParser.parseAndCreateViaApi( context,
                                                                                          repository,
                                                                                          store,
                                                                                          appName,
                                                                                          changeComment );

                            Property property = propertyKey.getPropertyForContext( ctxLevels );

                            boolean isDelete = valueJson.has( "opp" )
                                               ? "delete".equalsIgnoreCase( valueJson.get( "opp" ).getAsString() )
                                               : false;

                            if ( null == property )
                            {
                                if ( isDelete )
                                {
                                    continue;
                                }

                                property = new Property( repository );
                                property.setPropertyKey( propertyKey );
                                property.setActive( true );
                            }
                            else if ( isDelete )
                            {
                                String pass = propertyKey.isSecure()
                                              ? passwords.get( propertyKey.getSecurityProfile().getName() )
                                              : null;

                                store.deleteProperty( appName, repository, token, property.getId(), pass, changeComment );
                                continue;
                            }

                            if ( valueJson.has( "value" ) )
                            {
                                String pass = propertyKey.isSecure()
                                              ? passwords.get( propertyKey.getSecurityProfile().getName() )
                                              : null;

                                String value = "";
                                switch ( propertyKey.getValueDataType() )
                                {
                                    case FileEmbed:
                                    case FileRef:
                                        if ( valueJson.get( "value" ).isJsonNull() )
                                        {
                                            throw new ConfigException( Error.Code.FILE_NOT_FOUND, entry );
                                        }

                                        value = valueJson.get( "value" ).getAsString();
                                        AbsoluteFilePath absoluteFilePath = store.getAbsFilePath( repository, value, null );
                                        if ( null == absoluteFilePath )
                                        {
                                            throw new ConfigException( Error.Code.FILE_NOT_FOUND, entry );
                                        }

                                        property.setAbsoluteFilePath( absoluteFilePath );
                                        break;

                                    case List:
                                    case Map:
                                        if ( valueJson.get( "value" ).isJsonNull() )
                                        {
                                            property.setValue( null, pass );
                                        }
                                        else
                                        {
                                            property.setValue( valueJson.get( "value" ).getAsString(), pass );
                                        }
                                        break;

                                    default:
                                        if ( valueJson.get( "value" ).isJsonNull() )
                                        {
                                            property.setValue( null, pass );
                                        }
                                        else
                                        {
                                            property.setValue( valueJson.get( "value" ).getAsString(), pass );
                                        }
                                        break;
                                }
                            }

                            if ( valueJson.has( "active" ) )
                            {
                                property.setActive( valueJson.get( "active" ).getAsBoolean() );
                            }

                            property.setContext( ctxLevels );
                            store.saveProperty( appName, repository, token, property, changeComment );
                        }
                        catch ( ConfigException e )
                        {
                            throw new ConfigException( e.getErrorCode(), entry );
                        }
                    }
                }

                if ( propertyKey.dirty )
                {
                    store.savePropertyKey( appName, repository, token, propertyKey, changeComment );
                }
            }
        }

        store.commit();
    }
}
