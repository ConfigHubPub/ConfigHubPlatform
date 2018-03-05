/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.core.repository;

import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.store.APersisted;
import com.confighub.core.store.diff.AbsoluteFilePathDiffTracker;
import com.confighub.core.utils.MimeType;
import com.confighub.core.utils.Utils;
import com.google.gson.JsonObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.envers.AuditMappedBy;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Table(
    uniqueConstraints = @UniqueConstraint(columnNames = {"absPath", "repositoryId"}),
    indexes = {@Index(name = "AFP_repoIndex", columnList = "id, repositoryId, absPath")}
)
@NamedQueries({
    @NamedQuery(name = "AbsFilePath.getByAbsPath",
                query = "SELECT k FROM AbsoluteFilePath k WHERE repository=:repository AND absPath=:absPath"),
    @NamedQuery(name = "AbsFilePath.searchByAbsPath",
            query = "SELECT k FROM AbsoluteFilePath k WHERE repository=:repository AND absPath LIKE :absPath"),
    @NamedQuery(name = "AbsFilePath.searchByPath",
            query = "SELECT k FROM AbsoluteFilePath k WHERE repository=:repository AND path LIKE :path")

})
@Audited
@EntityListeners({AbsoluteFilePathDiffTracker.class})
public class AbsoluteFilePath
        extends APersisted
{
    private static final Logger log = LogManager.getLogger(AbsoluteFilePath.class);

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "filename", nullable = false)
    private String filename;
    @Column(name = "path")
    private String path;

    @Column(name = "absPath", nullable = false)
    private String absPath;

    @ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.REFRESH, CascadeType.PERSIST })
    @JoinColumn(nullable = false, name = "repositoryId")
    private Repository repository;

    @AuditMappedBy(mappedBy="absFilePath")
    @OneToMany(fetch = FetchType.LAZY,
               cascade = { CascadeType.REMOVE, CascadeType.PERSIST, CascadeType.REFRESH },
               mappedBy = "absFilePath")
    private Set<RepoFile> files;

    @AuditMappedBy(mappedBy="absoluteFilePath")
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "absoluteFilePath")
    private Set<Property> properties;

    /**
     * Value is set by the UI resolver and read by the UI.  This way we save additional DB
     * query for properties in order to count assignments.
     */
    public transient int fileCount = 0;


    // --------------------------------------------------------------------------------------------
    // Construction
    // --------------------------------------------------------------------------------------------
    protected AbsoluteFilePath() {}

    public AbsoluteFilePath(final Repository repository, final String path, final String filename)
    {
        this.repository = repository;
        this.path = path;
        this.filename = filename;
        this.files = new HashSet<>();
        this.absPath = Utils.isBlank(path) ? this.filename : this.path + "/" + this.filename;
    }

    // --------------------------------------------------------------------------------------------
    // File management
    // --------------------------------------------------------------------------------------------

    protected void addFile(final RepoFile file)
    {
        if (null == file)
            return;

        this.files.add(file);
    }

    public void removeFile(final RepoFile file)
    {
        if (null == file)
            return;

        this.files.remove(file);
    }

    // --------------------------------------------------------------------------------------------
    // Validation before saving
    // --------------------------------------------------------------------------------------------

    /**
     * Rules for a correct property key
     *
     * 1. Filename cannot be blank;
     * 2. Path has to be valid format
     *
     * @throws ConfigException
     */
    @PreUpdate
    @PrePersist
    public void enforce()
            throws ConfigException
    {
        if (Utils.isBlank(this.filename))
            throw new ConfigException(Error.Code.BLANK_NAME);

        if (Utils.isBlank(this.path))
            this.path = null;

        if (!Utils.isPathAndFileValid(this.path))
            throw new ConfigException(Error.Code.ILLEGAL_CHARACTERS);
    }


    // --------------------------------------------------------------------------------------------
    // POJO Ops
    // --------------------------------------------------------------------------------------------

    @Override
    public String toString()
    {
        return String.format("AbsoluteFilePath[%d]: %s", this.id, this.absPath);
    }

    @Override
    public boolean equals(Object other)
    {
        if (null == other)
            return false;

        if (!(other instanceof AbsoluteFilePath))
            return false;

        AbsoluteFilePath o = (AbsoluteFilePath)other;
        return o.getAbsPath().equals(this.getAbsPath()) && o.getRepository().equals(this.getRepository());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.repository.getName(), this.getAbsPath());
    }

    public JsonObject toJson()
    {
        JsonObject json = new JsonObject();
        json.addProperty("id", this.id);
        json.addProperty("filename", this.filename);
        json.addProperty("path", this.path);

        return json;
    }

    public RepoFile getFileForContext(Set<Level> context)
    {
        if (null == this.files || this.files.size() == 0)
            return null;

        for (RepoFile repoFile : this.files)
        {
            if (repoFile.isActive() && CollectionUtils.isEqualCollection(context, repoFile.getContext()))
                return repoFile;
        }

        return null;
    }


    @Override
    public Long getId()
    {
        return this.id;
    }

    public Repository getRepository()
    {
        return this.repository;
    }

    public String getAbsPath()
    {
        return absPath;
    }

    public String getPath()
    {
        return path;
    }

    public String getFilename()
    {
        return filename;
    }

    public Set<Property> getProperties()
    {
        return properties;
    }

    public String getContentType() {
        int li = filename.lastIndexOf(".");
        if (li < 0) return "text/plain";

        return MimeType.getContentType(filename.substring(li+1, filename.length()));
    }

    public void setFilename(String filename)
    {
        this.filename = filename;
        this.absPath = Utils.isBlank(path) ? this.filename : this.path + "/" + this.filename;
    }

    public void setPath(String path)
    {
        this.path = path;
        this.absPath = Utils.isBlank(path) ? this.filename : this.path + "/" + this.filename;
    }

    public Set<RepoFile> getFiles()
    {
        return files;
    }

    @Override
    public ClassName getClassName()
    {
        return ClassName.AbsoluteFilePath;
    }
}
