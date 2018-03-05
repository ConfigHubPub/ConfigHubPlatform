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
import com.confighub.core.store.diff.LevelDiffTracker;
import com.confighub.core.utils.Utils;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.envers.AuditMappedBy;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.*;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Table(
        uniqueConstraints=@UniqueConstraint(columnNames = {"name", "depth", "repositoryId"}),
        indexes = {@Index(name = "LVL_repoIndex", columnList = "id, repositoryId")}
)
@NamedQueries(
{
    @NamedQuery(name = "Level.getByName",
        query = "SELECT l FROM Level l WHERE repository=:repository AND UPPER(name)=:name AND depth=:depth"),

    @NamedQuery(name = "Level.getForDepth",
        query = "SELECT l FROM Level l WHERE repository=:repository AND depth=:depth"),

    @NamedQuery(name = "Level.depthCount",
        query = "SELECT COUNT(l) FROM Level l WHERE repository=:repository AND depth=:depth"),

})
@EntityListeners({ LevelDiffTracker.class })
@Audited
public class Level
        extends APersisted
        implements Comparable<Level>
{
    private static final Logger log = LogManager.getLogger(Level.class);
    public enum LevelType { Standalone, Group, Member}

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Depth depth;

    @AuditMappedBy(mappedBy="context")
    @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.REFRESH, CascadeType.PERSIST }, mappedBy = "context")
    private Set<Property> properties;

    @AuditMappedBy(mappedBy="context")
    @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.REFRESH, CascadeType.PERSIST }, mappedBy = "context")
    private Set<RepoFile> files;

    @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.REFRESH, CascadeType.PERSIST })
    private Set<Level> members;

    @AuditMappedBy(mappedBy="members")
    @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.REFRESH }, mappedBy = "members")
    private Set<Level> groups;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LevelType type;

    @ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.REFRESH })
    @JoinColumn(nullable = false, name="repositoryId")
    private Repository repository;

    // --------------------------------------------------------------------------------------------
    // Construction
    // --------------------------------------------------------------------------------------------
    protected Level() {}

    public Level(final Repository repository, final Depth depth)
    {
        this.repository = repository;
        this.depth = depth;
    }

    // --------------------------------------------------------------------------------------------
    // Validation before saving
    // --------------------------------------------------------------------------------------------

    @PreRemove
    public void preRemove()
            throws ConfigException
    {
        if (this.isGroup())
        {
            this.getMembers().forEach(m -> {
                if (m.getGroups().size() == 1)
                    m.setType(LevelType.Standalone);
            });
        }
        else if (this.isMember())
            this.getGroups().forEach(g -> g.removeMember(this));
    }

    /**
     * Requirements for level:
     * 1. Name, depth and type cannot be blank
     * 2. Within a repository and within a depth -> level name is unique -> constraint enforced
     */
    @PreUpdate
    @PrePersist
    public void enforce()
            throws ConfigException
    {
        if (!Utils.isComponentNameValid(this.name))
            throw new ConfigException(Error.Code.ILLEGAL_CHARACTERS);

        // Depth has to be set
        if (null == this.depth)
            throw new ConfigException(Error.Code.MISSING_PARAMS);

        if (this.depth.getIndex() > this.repository.getDepth().getIndex())
            throw new ConfigException(Error.Code.CONTEXT_SCOPE_MISMATCH);

        if (null != this.members && this.members.size() == 0)
            this.members = null;

        if (this.isGroup() && null != this.properties)
        {
            // extract related clusters
            Collection<Level> clusters = new HashSet<>();
            clusters.add(this);

            for (Level member : this.members)
            {
                if (null != member.groups)
                    clusters.addAll(member.groups);
            }

            if (clusters.size() > 1)
            {
                // organize keys
                Map<PropertyKey, List<Property>> keyMap = new HashMap<>();
                for (Level cluster : clusters)
                {
                    for (Property property : cluster.getProperties())
                    {
                        List<Property> props = keyMap.get(property.getPropertyKey());
                        if (null == props)
                        {
                            props = new ArrayList<>();
                            keyMap.put(property.getPropertyKey(), props);
                        }

                        props.add(property);
                    }
                }

                EnumSet<Depth> depths = repository.getDepth().getDepths();
                for (PropertyKey key : keyMap.keySet())
                {
                    List<Property> props = keyMap.get(key);

                    // if there are less than 2 properties, there's nothing to compare
                    if (props.size() < 2) continue;

                    for (int i=0; i<props.size()-1; i++)
                    {
                        Property propA = props.get(i);
                        for (int j=i+1; j<props.size(); j++)
                        {
                            Property propB = props.get(j);

                            if (propA.getContextWeight() != propB.getContextWeight())
                                continue;

                            Map<Depth, Level> aMap = propA.getContextMap();
                            Map<Depth, Level> bMap = propB.getContextMap();

                            boolean conflict = true;
                            for (Depth depth : depths)
                            {
                                if (this.depth.getPlacement() == depth.getPlacement())
                                {
                                    if (!clusters.contains(aMap.get(depth)) ||
                                        !clusters.contains(bMap.get(depth)))
                                    {
                                        conflict = false;
                                        break;
                                    }
                                }
                                else
                                {
                                    if (!Utils.equal(aMap.get(depth), bMap.get(depth)))
                                    {
                                        conflict = false;
                                        break;
                                    }
                                }
                            }

                            if (conflict)
                            {
                                // ToDo: include JSON of conflicting values
                                throw new ConfigException(Error.Code.GROUP_CONFLICT);
                            }
                        }

                    }
                }

            }
        }

    }


    // --------------------------------------------------------------------------------------------
    // POJO Ops
    // --------------------------------------------------------------------------------------------

    @Override
    public String toString()
    {
        return String.format("Level[%d]: %s in %s : %s",
                             this.id,
                             this.name,
                             this.depth,
                             this.type);
    }

    @Override
    public int compareTo(Level other)
    {
        if (this.getContextScore() > other.getContextScore()) return 1;
        if (this.getContextScore() < other.getContextScore()) return -1;
        return 0;
    }

    public int getContextScore()
    {
        return this.depth.getScore(this.isGroup());
    }

    public int getContextPlacement()
    {
        return this.depth.getPlacement();
    }

    public JsonObject toJson()
    {
        JsonObject json = new JsonObject();
        json.addProperty("id", this.getId());
        json.addProperty("name", this.getName());
        json.addProperty("type", this.getType().name());

        return json;
    }

    @Override
    public boolean equals(Object o)
    {
        if (null == o)
            return false;

        Level other = (Level)o;

        return this.getRepository().getId().equals(other.getRepository().getId()) &&
               this.depth.equals(other.depth) &&
                this.name.equalsIgnoreCase(other.name);
    }

    @Override
    public int hashCode()
    {
        // Because envers will store deleted item without any data, we have to be ready for this.
        if (Utils.anyNull(depth, repository))
            return this.id.intValue();

        return Objects.hash(this.name, depth.name(), repository.getName());
    }


    // --------------------------------------------------------------------------------------------
    // Group / Member / Standalone
    // --------------------------------------------------------------------------------------------

    public void addMember(Level member)
    {
        if (null == member)
            return;

        if (member.isGroup())
            throw new ConfigException(Error.Code.GROUP_TO_GROUP_ASSIGNMENT);

        if (null == this.members)
            this.members = new HashSet<>();

        this.members.add(member);

        member.setType(LevelType.Member);
        this.type = LevelType.Group;
    }

    public void removeMember(Level member)
    {
        if (null == this.members || null == member)
            return;

        this.members.remove(member);

        if (this.members.size() == 0)
        {
            this.properties.stream().forEach(p -> p.updateContextString());
            this.type = LevelType.Standalone;
        }
    }

    // --------------------------------------------------------------------------------------------
    // Setters and getters
    // --------------------------------------------------------------------------------------------

    public Long getId()
    {
        return this.id;
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Depth getDepth()
    {
        return this.depth;
    }

    public void setDepth(Depth depth)
    {
        this.depth = depth;
    }

    public Repository getRepository()
    {
        return this.repository;
    }

    public Set<Property> getProperties()
    {
        return this.properties;
    }

    public Set<RepoFile> getFiles()
    {
        return files;
    }

    public void setMembers(Set<Level> members)
    {
        this.members = members;
        if (null != members)
        {
            members.forEach(m -> m.setType(LevelType.Member));
            this.type = LevelType.Group;
        } else {
            this.type = LevelType.Standalone;
        }
    }

    public void setType(LevelType type)
    {
        this.type = type;
    }

    public LevelType getType()
    {
        return this.type;
    }

    public Set<Level> getMembers()
    {
        return this.members;
    }

    public Set<Level> getGroups()
    {
        return this.groups;
    }

    public boolean isMember()
    {
        return LevelType.Member == this.type;
    }

    public boolean isGroup()
    {
        return LevelType.Group == this.type;
    }

    public boolean isStandalone()
    {
        return LevelType.Standalone == this.type;
    }

    @Override
    public ClassName getClassName() {
        return ClassName.ContextItem;
    }
}
