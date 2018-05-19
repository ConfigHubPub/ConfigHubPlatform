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

package com.confighub.core.resolver;

import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.repository.*;
import com.confighub.core.store.Store;
import com.confighub.core.user.UserAccount;
import com.confighub.core.utils.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class RepositoryFilesResolver
        extends AResolver
{
    private static final Logger log = LogManager.getLogger("RepositoryFilesResolver");
    private final boolean isClient;
    private final UserAccount user;
    private final String searchQuery;
    private final boolean searchResolved;

    protected RepositoryFilesResolver(Store store)
    {
        super(store);
        this.isClient = true;
        this.user = null;
        this.searchQuery = null;
        this.searchResolved = false;
    }


    protected RepositoryFilesResolver(Store store,
                                      final UserAccount user,
                                      final boolean isClient,
                                      final String searchQuery,
                                      final boolean searchResolved)
    {
        super(store);
        this.isClient = isClient;
        this.user = user;
        this.searchQuery = searchQuery;
        this.searchResolved = searchResolved;
    }

    protected RepoFile resolveFullContextFilePath(final Context context, final AbsoluteFilePath absPath)
        throws ConfigException
    {
        if (null == absPath)
            return null;

        if (Utils.anyNull(context))
            throw new ConfigException(Error.Code.MISSING_PARAMS);

        if (!context.isFullContext())
            throw new ConfigException(Error.Code.PARTIAL_CONTEXT);

        RepoFile heaviest = null;

        for (RepoFile file : absPath.getFiles())
        {
            if (isContextualMatchAudit(file.getDepthMap(), context))
            {
                if (file.isActive())
                {
                    if (null == heaviest || file.getContextWeight() > heaviest.getContextWeight())
                        heaviest = file;
                }
            }
        }

        return heaviest;
    }


    protected Collection<RepoFile> resolvePartialContextFilePath(final Context context, final AbsoluteFilePath path)
            throws ConfigException
    {
        if (Utils.anyNull(context, path))
            throw new ConfigException(Error.Code.MISSING_PARAMS);

        Map<AbsoluteFilePath, Collection<RepoFile>> filesByPath = new HashMap<>();
        filesByPath.put(path, new ArrayList<>());

        for (RepoFile file : path.getFiles())
        {
            if (!file.isActive())
                continue;

            if (isContextualMatchAudit(file.getDepthMap(), context))
                filesByPath.get(path).add(file);
        }

        Map<AbsoluteFilePath, Collection<RepoFile>> resolved = partialContext(filesByPath, context.wildcards);
        return resolved.get(path);
    }

    protected Map<AbsoluteFilePath, RepoFile> resolveClient(final Context context)
            throws ConfigException
    {
        if (Utils.anyNull(context))
            throw new ConfigException(Error.Code.MISSING_PARAMS);

        if (!context.isFullContext())
            throw new ConfigException(Error.Code.PARTIAL_CONTEXT);

        Map<AbsoluteFilePath, Collection<RepoFile>> filesByPath = getFilesByPath(context);
        Map<AbsoluteFilePath, RepoFile> resolved = new HashMap<>();

        for (AbsoluteFilePath absoluteFilePath : filesByPath.keySet())
        {
            RepoFile heaviest = null;

            for (RepoFile file : filesByPath.get(absoluteFilePath))
            {
                if (null == heaviest || file.getContextWeight() > heaviest.getContextWeight())
                    heaviest = file;
            }

            if (null != heaviest)
                resolved.put(absoluteFilePath, heaviest);
        }

        return resolved;
    }


    protected Map<AbsoluteFilePath, Collection<RepoFile>> resolve(final Context context)
            throws ConfigException
    {
        if (Utils.anyNull(context))
            throw new ConfigException(Error.Code.MISSING_PARAMS);

        Map<AbsoluteFilePath, Collection<RepoFile>> filesByPath = getFilesByPath(context);

        // If this is a search across all files (not context specific), return all search results
        if ((!Utils.isBlank(searchQuery) && !searchResolved) || context.all)
            return filesByPath;

        Map<AbsoluteFilePath, Collection<RepoFile>> resolved;

        if (context.isFullContext())
            resolved = fullContext(filesByPath);
        else
            resolved = partialContext(filesByPath, context.wildcards);

        return resolved;
    }

    protected static Map<AbsoluteFilePath, Collection<RepoFile>>
        fullContext(Map<AbsoluteFilePath, Collection<RepoFile>> filesByPath)
            throws ConfigException
    {
        // final filtering
        Map<AbsoluteFilePath, Collection<RepoFile>> resolved = new HashMap<>();
        for (AbsoluteFilePath absoluteFilePath : filesByPath.keySet())
        {
            List<RepoFile> matchedFiles = new ArrayList<>();
            RepoFile heaviest = null;

            for (RepoFile file : filesByPath.get(absoluteFilePath))
            {
                if (!file.isActive())
                    matchedFiles.add(file);
                else if (null == heaviest || file.getContextWeight() > heaviest.getContextWeight())
                    heaviest = file;
            }

            if (null != heaviest)
                matchedFiles.add(heaviest);

            resolved.put(absoluteFilePath, matchedFiles);
        }

        return resolved;
    }

    public static RepoFile fullContextResolveForPath(final AbsoluteFilePath absoluteFilePath, final Context context)
            throws ConfigException
    {
        if (Utils.anyNull(absoluteFilePath, context))
             return null;

        Set<RepoFile> files = absoluteFilePath.getFiles();
        List<RepoFile> matches = new ArrayList<>();

        for (RepoFile file : files)
        {
            if (!file.isActive())
                continue;

            boolean match = isContextualMatchAudit(file.getDepthMap(), context);
            if (match)
                matches.add(file);
        }

        RepoFile heaviest = null;

        for (RepoFile file : matches)
        {
            if (null == heaviest || file.getContextWeight() > heaviest.getContextWeight())
                heaviest = file;
        }

        return heaviest;
    }


    protected static Map<AbsoluteFilePath, Collection<RepoFile>>
        partialContext(Map<AbsoluteFilePath, Collection<RepoFile>> filesByPath, final Set<Depth> wildcards)
            throws ConfigException
    {
        Map<AbsoluteFilePath, Collection<RepoFile>> resolved = new HashMap<>();

        for (AbsoluteFilePath absoluteFilePath : filesByPath.keySet())
        {
            Set<RepoFile> matchedFiles = new HashSet<>();
            RepoFile heaviest = null;

            Collection<RepoFile> absPathFiles = filesByPath.get(absoluteFilePath);
            for (RepoFile file : absPathFiles)
            {
                boolean fileIsWildcardMatch = false;
                Map<String, LevelCtx> fileDepthMap = file.getDepthMap();
                for (Depth depth : wildcards)
                {
                    // property has context &&
                    // property has context at the depth context is wildcard-ed
                    if (null != fileDepthMap &&
                            fileDepthMap.containsKey(String.valueOf(depth.getPlacement())))
                    {
                        fileIsWildcardMatch = true;
                        matchedFiles.add(file);
                        break;
                    }
                }

                // Properties that are not matched based on wildcards in the context,
                // are treated as full-context resolution, and are therefore weight
                // compared.
                if (!fileIsWildcardMatch)
                {
                    if (!file.isActive())
                        matchedFiles.add(file);
                    if (null == heaviest || file.getContextWeight() > heaviest.getContextWeight())
                        heaviest = file;
                }
            }

            if (null != heaviest)
                matchedFiles.add(heaviest);

            resolved.put(absoluteFilePath, matchedFiles);
        }

        return resolved;
    }


    private Map<AbsoluteFilePath, Collection<RepoFile>> getFilesByPath(Context context)
            throws ConfigException
    {
        Collection<RepoFile> files;

        if (this.isClient)
            files = store.getRepoFilesForAPI(context.repository, context.date);
        else
            files = store.getRepoFiles(context.repository, user, searchQuery, context.date);

        Map<AbsoluteFilePath, Collection<RepoFile>> filesByPath = new HashMap<>();

        if ((!Utils.isBlank(searchQuery) && !searchResolved) || context.all)
        {
            for (RepoFile file : files)
            {
                AbsoluteFilePath absoluteFilePath = file.getAbsFilePath();
                boolean inMap = filesByPath.containsKey(file.getAbsFilePath());
                if (!inMap)
                    filesByPath.put(absoluteFilePath, new ArrayList<>());
                filesByPath.get(absoluteFilePath).add(file);
            }
        }
        else
        {
            for (RepoFile file : files)
            {
                if (this.isClient && !file.isActive())
                    continue;

                boolean match = isContextualMatchAudit(file.getDepthMap(), context);
                boolean inMap = filesByPath.containsKey(file.getAbsFilePath());

                AbsoluteFilePath absoluteFilePath = file.getAbsFilePath();

                if (!inMap && (context.all || match))
                    filesByPath.put(absoluteFilePath, new ArrayList<>());

                if (match)
                    filesByPath.get(absoluteFilePath).add(file);
            }
        }

        return filesByPath;
    }

}
