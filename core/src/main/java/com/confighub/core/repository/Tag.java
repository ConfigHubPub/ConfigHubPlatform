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
import com.confighub.core.store.diff.TagDiffTracker;
import com.confighub.core.utils.DateTimeUtils;
import com.confighub.core.utils.Utils;
import com.google.gson.JsonObject;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.Date;

@Entity
@NamedQueries({
    @NamedQuery(name = "Tag.getByName",
                query = "SELECT t FROM Tag t WHERE repository.id=:repositoryId AND name=:name"),
    @NamedQuery(name = "Tag.getAll",
            query = "SELECT t FROM Tag t WHERE repository=:repository"),

})
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Table(uniqueConstraints=@UniqueConstraint(columnNames = {"name", "repositoryId"}))
@Audited
@EntityListeners({ TagDiffTracker.class})
public class Tag
        extends APersisted
{
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.REFRESH })
    @JoinColumn(nullable = false, name = "repositoryId")
    private Repository repository;

    @Column(name="readme")
    private String readme;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Long ts;

    protected Tag() {}

    public Tag(final Repository repository)
    {
        this.repository = repository;
    }

    @Override
    public String toString()
    {
        return String.format("Tag[%d] %s | %d", this.id, this.name, this.ts);
    }

    public JsonObject toJson()
    {
        JsonObject json = new JsonObject();
        json.addProperty("name", this.name);
        json.addProperty("readme", null == this.readme ? "" : this.readme);
        json.addProperty("ts", this.ts);
        json.addProperty("date", DateTimeUtils.toISO8601(new Date(this.ts)));


        return json;
    }

    // --------------------------------------------------------------------------------------------
    // Setters and getters
    // --------------------------------------------------------------------------------------------


    @Override
    public Long getId()
    {
        return id;
    }

    public String getReadme()
    {
        return readme;
    }

    public void setReadme(String readme)
    {
        this.readme = readme;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
        throws ConfigException
    {
        if (!Utils.isKeyValid(name))
            throw new ConfigException(Error.Code.ILLEGAL_CHARACTERS);

        this.name = name;
    }

    public Long getTs()
    {
        return ts;
    }

    public void setTs(Long ts)
    {
        this.ts = ts;
    }

    @Override
    public ClassName getClassName() {
        return ClassName.Tag;
    }

}
