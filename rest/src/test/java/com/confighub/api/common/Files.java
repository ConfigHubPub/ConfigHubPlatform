package com.confighub.api.common;

import com.confighub.api.repository.user.files.GetRepoFiles;
import com.confighub.api.repository.user.files.SaveConfigFile;
import org.apache.commons.io.IOUtils;

import javax.ws.rs.core.Response;
import java.io.IOException;

import static org.junit.Assert.fail;

/**
 * Common file operations
 */
public class Files
{
    /**
     * Create or update a file as per @Path("/saveConfigFile")
     *
     * @param account
     * @param repositoryName
     * @param userToken
     * @param path
     * @param name
     * @param id
     * @param content
     * @param fileContext
     * @param active
     * @param changeComment
     * @param currentPassword
     * @param newProfilePassword
     * @param spName
     * @param renameAll
     * @param updateRefs
     * @return Response
     */
    public static Response saveOrUpdateFile(final String account,
                                            final String repositoryName,
                                            final String userToken,
                                            final String path,
                                            final String name,
                                            final Long id,
                                            final String content,
                                            final String fileContext,
                                            final boolean active,
                                            final String changeComment,
                                            final String currentPassword,
                                            final String newProfilePassword,
                                            final String spName,
                                            final boolean renameAll,
                                            final boolean updateRefs)
    {
        SaveConfigFile fileAPI = new SaveConfigFile();
        return fileAPI.saveOrUpdate(account,
                                    repositoryName,
                                    userToken,
                                    path,
                                    name,
                                    id,
                                    content,
                                    fileContext,
                                    active,
                                    changeComment,
                                    currentPassword,
                                    newProfilePassword,
                                    spName,
                                    renameAll,
                                    updateRefs);

    }


    /**
     * Get files as per @Path("/getRepoFiles")
     *
     * @param account
     * @param repositoryName
     * @param contextString
     * @param all
     * @param ts
     * @param tagLabel
     * @param searchTerm
     * @param searchResolved
     * @param userToken
     * @return Response
     */
    public static Response getRepositoryFiles(final String account,
                                              final String repositoryName,
                                              final String contextString,
                                              final boolean all,
                                              final Long ts,
                                              final String tagLabel,
                                              final String searchTerm,
                                              final boolean searchResolved,
                                              final String userToken)
    {
        GetRepoFiles filesAPI = new GetRepoFiles();
        return filesAPI.get(account,
                            repositoryName,
                            contextString,
                            all,
                            ts,
                            tagLabel,
                            searchTerm,
                            searchResolved,
                            userToken);
    }


    /**
     * Read local file from resources folder with a path relative to the test.
     *
     * @param testClass
     * @param file
     * @return String content of the file
     */
    public static String readLocalFile(Class testClass, String file)
    {
        try
        {
            return IOUtils.toString(testClass.getResourceAsStream(file), "UTF-8");
        }
        catch (IOException e)
        {
            fail(e.getMessage());
            return "";
        }
    }
}
