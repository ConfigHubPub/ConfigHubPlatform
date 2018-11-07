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

package com.confighub.core.repository;

import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.resolver.Context;
import com.confighub.core.store.APersisted;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.*;


@Audited
@MappedSuperclass
public abstract class AContextAwarePersistent
      extends APersisted
{
    private static final Logger log = LogManager.getLogger( AContextAwarePersistent.class );


    @ManyToOne( fetch = FetchType.LAZY,
                cascade = { CascadeType.REFRESH,
                            CascadeType.PERSIST } )
    @JoinColumn( nullable = false,
                 name = "repositoryId" )
    protected Repository repository;

    @ManyToMany( fetch = FetchType.LAZY,
                 cascade = { CascadeType.REFRESH } )
    protected Set<CtxLevel> context;

    // ENVERS optimization
    @Lob
    @Type( type = "org.hibernate.type.TextType" )
    @Column( nullable = false,
             columnDefinition = "TEXT" )
    protected String contextJson;

    @JoinColumn( nullable = false )
    protected int contextWeight;

    @Column( name = "active" )
    protected boolean active;

    public transient boolean isEditable = true;

    public transient Context.PropertyType type;

    // --------------------------------------------------------------------------------------------
    // Context
    // --------------------------------------------------------------------------------------------


    public JsonArray getContextJsonObj()
    {
        return new Gson().fromJson( this.contextJson, JsonArray.class );
    }


    private transient Map<String, LevelCtx> depthMap;


    public void resetTransient()
    {
        depthMap = null;
    }


    public Map<String, LevelCtx> getDepthMap()
          throws ConfigException
    {
        if ( null == depthMap )
        {
            depthMap = new HashMap<>();

            JsonArray json = getContextJsonObj();

            for ( int i = 0; i < json.size(); i++ )
            {
                JsonObject vo = json.get( i ).getAsJsonObject();
                if ( vo.has( "w" ) )
                {
                    CtxLevel.LevelType type;
                    switch ( vo.get( "t" ).getAsInt() )
                    {
                        case 1:
                            type = CtxLevel.LevelType.Member;
                            break;
                        case 2:
                            type = CtxLevel.LevelType.Group;
                            break;
                        default:
                            type = CtxLevel.LevelType.Standalone;
                            break;
                    }

                    depthMap.put( vo.get( "p" ).getAsString(), new LevelCtx( vo.get( "n" ).getAsString(), type ) );
                }
            }
        }

        return depthMap;
    }

    // --------------------------------------------------------------------------------------------
    // POJO Ops
    // --------------------------------------------------------------------------------------------


    void validateContext()
          throws ConfigException
    {
        if ( null != this.context && context.size() > 1 )
        {
            Map<Depth, CtxLevel> depthMap = new HashMap<>();
            for ( CtxLevel ctxLevel : this.context )
            {
                if ( depthMap.containsKey( ctxLevel.getDepth() ) )
                {
                    throw new ConfigException( Error.Code.PROP_CONTEXT_DUPLICATE_DEPTH );
                }

                depthMap.put( ctxLevel.getDepth(), ctxLevel );
            }
        }
    }


    void crossClusterValidation( List<AContextAwarePersistent> conflictCandidates )
          throws ConfigException
    {
        if ( null != conflictCandidates )
        {
            // Now that there are conflict candidates, find all proxy clusters and organize
            // them into a single map "mask"

            Map<Depth, Collection<CtxLevel>> mask = null;
            for ( CtxLevel ci : this.context )
            {
                if ( !ci.isGroup() )
                {
                    continue;
                }

                // Btw, I'm not adding "ci" into the "mask" because if there was another match,
                // this would be caught by rule #3.

                // ci is a cluster.  find other proxy clusters, if any
                for ( CtxLevel ciNode : ci.getMembers() )
                {
                    if ( null == ciNode.getGroups() || ciNode.getGroups().size() == 1 )
                    {
                        continue;
                    }

                    for ( CtxLevel proxyCluster : ciNode.getGroups() )
                    {
                        if ( proxyCluster.equals( ci ) || null == proxyCluster.getFiles() )
                        {
                            continue;
                        }

                        if ( null == mask )
                        {
                            mask = new HashMap<>();
                        }

                        Collection<CtxLevel> depthClusters = mask.get( ci.getDepth() );
                        if ( null == depthClusters )
                        {
                            depthClusters = new HashSet<>();
                            mask.put( ci.getDepth(), depthClusters );
                        }

                        depthClusters.add( proxyCluster );
                    }
                }
            }

            // If there are proxy-clusters, go through all conflict candidates to check for real conflicts.
            if ( null != mask )
            {
                EnumSet<Depth> depths = repository.getDepth().getDepths();
                Map<Depth, CtxLevel> thisContextMap = this.getContextMap();

                boolean conflict = true;
                for ( AContextAwarePersistent file : conflictCandidates )
                {
                    Map<Depth, CtxLevel> otherContextMap = file.getContextMap();

                    for ( Depth depth : depths )
                    {
                        if ( mask.containsKey( depth ) )
                        {
                            if ( !mask.get( depth ).contains( otherContextMap.get( depth ) ) )
                            {
                                // file we are comparing to, has a Level at this depth that is not
                                // one of the proxy-clusters
                                conflict = false;
                                break;
                            }
                        }
                        else
                        {
                            if ( !Utils.equal( thisContextMap.get( depth ), otherContextMap.get( depth ) ) )
                            {
                                // this and file have different level at this depth
                                conflict = false;
                                break;
                            }
                        }
                    }

                    if ( conflict )
                    {
                        log.info( "Conflict between..." );
                        log.info( "this: " + this );
                        log.info( "with: " + file );
                        throw new ConfigException( Error.Code.GROUP_CONFLICT, file.toJson() );
                    }
                }
            }
        }
    }


    void checkPropertyCircularReference( final Context context,
                                         final Property property,
                                         final HashMap<RepoFile, Property> breadcrumbs )
          throws ConfigException
    {
        if ( null == property.getAbsoluteFilePath() )
        {
            return;
        }

        Collection<RepoFile> files = context.resolvePartialContextFilePath( property.getAbsoluteFilePath() );

        if ( null == files || files.size() == 0 )
        {
            return;
        }

        for ( RepoFile file : files )
        {
            if ( !file.isActive() )
            {
                continue;
            }

            if ( breadcrumbs.containsKey( file ) )
            {
                Gson gson = new Gson();
                JsonArray steps = new JsonArray();

                for ( RepoFile f : breadcrumbs.keySet() )
                {
                    Property bp = breadcrumbs.get( f );
                    JsonObject step = new JsonObject();

                    step.addProperty( "filePath", f.getAbsPath() );
                    step.add( "fileContext", gson.fromJson( f.contextJson, JsonArray.class ) );
                    step.addProperty( "key", bp.getKey() );
                    step.add( "valueContext", gson.fromJson( bp.contextJson, JsonArray.class ) );
                    step.addProperty( "value", bp.getValue() );

                    steps.add( step );
                }

                JsonObject crumbs = new JsonObject();
                crumbs.add( "crumbs", steps );
                throw new ConfigException( Error.Code.FILE_CIRCULAR_REFERENCE, crumbs );
            }
            else
            {
                for ( PropertyKey key : file.getKeys() )
                {
                    if ( !PropertyKey.ValueDataType.FileEmbed.equals( key.getValueDataType() ) )
                    {
                        continue;
                    }

                    Collection<Property> properties = context.partialContextKeyResolver( key );
                    for ( Property p : properties )
                    {
                        breadcrumbs.put( file, p );
                        checkPropertyCircularReference( context, p, breadcrumbs );
                        breadcrumbs.remove( file );
                    }
                }
            }
        }
    }


    public abstract JsonObject toJson();


    private static CtxLevel getLevelAt( Depth d,
                                        Set<CtxLevel> context )
    {
        if ( null == context )
        {
            return null;
        }
        for ( CtxLevel l : context )
        {
            if ( l.getDepth() == d )
            {
                return l;
            }
        }
        return null;
    }


    public final void updateContextString()
    {
        this.contextWeight = 0;
        JsonArray json = new JsonArray();
        for ( Depth depth : this.repository.getDepth().getDepths() )
        {
            CtxLevel l;
            JsonObject ljson = new JsonObject();
            ljson.addProperty( "p", depth.getPlacement() );

            if ( null != ( l = getLevelAt( depth, this.context ) ) )
            {
                ljson.addProperty( "n", l.getName() );
                ljson.addProperty( "t", l.isStandalone() ? 0 : l.isMember() ? 1 : 2 );
                ljson.addProperty( "w", l.getContextScore() );

                this.contextWeight += l.getContextScore();
            }

            json.add( ljson );
        }

        this.contextJson = json.toString();
    }


    public transient Map<Depth, CtxLevel> contextMap;


    public Map<Depth, CtxLevel> getContextMap()
    {
        if ( null == contextMap )
        {
            contextMap = new HashMap<>();
            if ( null != context )
            {
                for ( CtxLevel l : this.context )
                {
                    contextMap.put( l.getDepth(), l );
                }
            }
        }
        return contextMap;
    }


    // --------------------------------------------------------------------------------------------
    // Getters & Setters
    // --------------------------------------------------------------------------------------------


    public Set<CtxLevel> getContext()
    {
        return context;
    }


    public void setContext( Collection<CtxLevel> context )
    {
        if ( null == context )
        {
            context = new HashSet<>();
        }

        if ( Utils.same( this.context, context ) )
        {
            return;
        }

        if ( null == context )
        {
            this.context = new HashSet<>();
        }
        else
        {
            this.context = new HashSet<>();

            for ( CtxLevel ctxLevel : context )
            {
                this.context.add( ctxLevel );
            }
        }

        updateContextString();
    }


    public int getContextWeight()
    {
        return this.contextWeight;
    }


    public String getContextJson()
    {
        return this.contextJson;
    }


    public Repository getRepository()
    {
        return this.repository;
    }


    public boolean isActive()
    {
        return this.active;
    }


    public void setActive( boolean active )
    {
        this.active = active;
    }
}
