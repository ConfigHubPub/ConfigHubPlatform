/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.core.repository;

import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.security.SecurityProfile;
import com.confighub.core.store.APersisted;
import com.confighub.core.store.diff.PropertyKeyDiffTracker;
import com.confighub.core.utils.Utils;
import com.google.gson.JsonObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.envers.AuditMappedBy;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = {"propertyKey", "repositoryId"}),
        indexes = {@Index(name = "PROP_KEY_repoIndex", columnList = "id, repositoryId")}
)
@NamedQueries({
    @NamedQuery(name = "Key.getByKey",
                query = "SELECT k FROM PropertyKey k WHERE repository=:repository AND UPPER(key)=:key"),

    @NamedQuery(name = "Key.getById",
                query = "SELECT k FROM PropertyKey k WHERE repository=:repository AND id=:keyId"),

    @NamedQuery(name = "Key.search",
                query = "SELECT k FROM PropertyKey k WHERE repository=:repository AND UPPER(k.key) LIKE :searchTerm"),

    @NamedQuery(name = "Key.getNonText",
        query = "SELECT k FROM PropertyKey k WHERE repository=:repository AND k.valueDataType <> :type"),

    @NamedQuery(name = "Key.getKeys",
            query = "SELECT k FROM PropertyKey k WHERE repository=:repository AND UPPER(k.key) IN :keys"),

    @NamedQuery(name = "Search.keysAndComments",
            query = "SELECT k FROM PropertyKey k WHERE k.repository=:repository AND " +
                    "(UPPER(k.key) LIKE :searchTerm OR UPPER(k.readme) LIKE :searchTerm)")

})
@Audited
@EntityListeners({PropertyKeyDiffTracker.class})
public class PropertyKey
        extends APersisted
{
    private static final Logger log = LogManager.getLogger(PropertyKey.class);

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "propertyKey", nullable = false)
    private String key;

    @Column(columnDefinition = "TEXT")
    private String readme;

    @Column(name="deprecated")
    private boolean deprecated;

    @Column(name="pushValueEnabled")
    private boolean pushValueEnabled = false;

    @ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.REFRESH, CascadeType.PERSIST })
    private SecurityProfile securityProfile;

    @ManyToMany(fetch = FetchType.LAZY,
            cascade = { CascadeType.REFRESH, CascadeType.PERSIST },
            mappedBy = "keys")
    private Set<RepoFile> files;

    @AuditMappedBy(mappedBy="propertyKey")
    @OneToMany(fetch = FetchType.LAZY,
               cascade = { CascadeType.REFRESH, CascadeType.PERSIST, CascadeType.REMOVE },
               mappedBy = "propertyKey")
    private Set<Property> properties;

    @ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.REFRESH, CascadeType.PERSIST })
    @JoinColumn(nullable = false, name = "repositoryId")
    private Repository repository;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public ValueDataType valueDataType;

    public enum ValueDataType
    {
        Text,
        Code,
        JSON,

        Boolean,

        Integer,
        Long,
        Double,
        Float,

        Map,
        List,

        FileRef,
        FileEmbed
    }

    /**
     * Value is set by the UI resolver and read by the UI.  This way we save additional DB
     * query for properties in order to count assignments.
     */
    public transient int propertyCount = 0;
    public transient boolean dirty = false;

    // --------------------------------------------------------------------------------------------
    // Construction
    // --------------------------------------------------------------------------------------------

    protected PropertyKey() {}

    public PropertyKey(final Repository repository, final String key)
    {
        this(repository, key, ValueDataType.Text);
    }

    public PropertyKey(final Repository repository, final String key, final ValueDataType valueDataType)
    {
        this.repository = repository;
        this.key = key.trim();
        this.properties = new HashSet<>();
        this.valueDataType = valueDataType;
    }

    // --------------------------------------------------------------------------------------------
    // Property management
    // --------------------------------------------------------------------------------------------

    public void addProperty(final Property property)
    {
        if (null == property)
            return;

        this.properties.add(property);
    }

    public void removeProperty(final Property property)
    {
        if (null == property)
            return;

        this.properties.remove(property);
    }

    // --------------------------------------------------------------------------------------------
    // Validation before saving
    // --------------------------------------------------------------------------------------------

    /**
     * Rules for a correct property key
     *
     * 1. Key cannot be blank;
     *
     * @throws ConfigException
     */
    @PreUpdate
    @PrePersist
    public void enforce()
            throws ConfigException
    {
        if (Utils.isBlank(this.key))
            throw new ConfigException(Error.Code.KEY_BLANK);

        if (!Utils.isKeyValid(this.key))
            throw new ConfigException(Error.Code.ILLEGAL_CHARACTERS);
    }

    // --------------------------------------------------------------------------------------------
    // POJO Ops
    // --------------------------------------------------------------------------------------------
    @Override
    public String toString()
    {
        return String.format("PropertyKey[%d]: %s", this.id, this.key);
    }

    @Override
    public boolean equals(Object other)
    {
        if (null == other)
            return false;

        if (!(other instanceof PropertyKey))
            return false;

        PropertyKey o = (PropertyKey)other;
        return o.getKey().equals(this.getKey()) && o.getRepository().equals(this.getRepository());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.repository.getName(), this.key);
    }

    public JsonObject toJson()
    {
        JsonObject json = new JsonObject();
        json.addProperty("id", this.getId());
        json.addProperty("key", this.getKey());
        json.addProperty("vdt", this.getValueDataType().name());
        json.addProperty("uses", this.getProperties().size());
        json.addProperty("deprecated", this.isDeprecated());

        return json;
    }

    public Property getPropertyForContext(Set<Level> context)
    {
        if (null == this.properties || this.properties.size() == 0)
            return null;

        for (Property property : this.properties)
        {
            if (property.isActive() && CollectionUtils.isEqualCollection(context, property.getContext()))
                return property;
        }

        return null;
    }

    // --------------------------------------------------------------------------------------------
    // Setters and getters
    // --------------------------------------------------------------------------------------------
    public Long getId()
    {
        return this.id;
    }

    public String getKey()
    {
        return this.key;
    }

    public void setKey(String key)
    {
        if (!dirty && !Utils.equal(key, this.key)) this.dirty = true;
        this.key = key;
    }

    public String getReadme()
    {
        return this.readme;
    }

    public void setReadme(String readme)
    {
        this.readme = readme;
    }

    public Set<Property> getProperties()
    {
        return this.properties;
    }

    public Repository getRepository()
    {
        return this.repository;
    }

    public boolean isDeprecated()
    {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated)
    {
        if (!dirty && deprecated == this.deprecated) this.dirty = true;
        this.deprecated = deprecated;
    }

    public Set<RepoFile> getFiles()
    {
        return files;
    }

    public SecurityProfile getSecurityProfile()
    {
        return securityProfile;
    }

    public boolean isPushValueEnabled()
    {
        return pushValueEnabled;
    }

    public void setPushValueEnabled(boolean pushValueEnabled)
    {
        if (!dirty && pushValueEnabled == this.pushValueEnabled) this.dirty = true;
        this.pushValueEnabled = pushValueEnabled;
    }

    /**
     *
     * @param securityProfile
     * @param existingSecretKey
     * @throws ConfigException
     */
    public void setSecurityProfile(final SecurityProfile securityProfile, final String existingSecretKey)
        throws ConfigException
    {
        if (!dirty && !Utils.equal(securityProfile, this.securityProfile)) this.dirty = true;

        if (null != this.securityProfile)
            this.getProperties().parallelStream().forEach(p -> p.decryptValue(existingSecretKey));

        this.securityProfile = securityProfile;
        this.getProperties().parallelStream().forEach(p -> p.setValue(p.getValue(), securityProfile.sk));
    }

    /**
     *
     * @param existingSecretKey
     * @throws ConfigException
     */
    public void removeSecurityProfile(final String existingSecretKey)
        throws ConfigException
    {
        if (!dirty && null != this.securityProfile) this.dirty = true;

        if (null != this.securityProfile)
            this.getProperties().parallelStream().forEach(p -> p.decryptValue(existingSecretKey));

        this.securityProfile = null;
    }

    public boolean isSecure()
    {
        return null != this.securityProfile;
    }

    public boolean isEncrypted()
    {
        return null != this.securityProfile && this.securityProfile.encryptionEnabled();
    }

    public ValueDataType getValueDataType()
    {
        return valueDataType;
    }

    public void setValueDataType(ValueDataType valueDataType)
    {
        if (!dirty && !Utils.equal(valueDataType, this.valueDataType)) this.dirty = true;
        this.valueDataType = valueDataType;
    }

    @Override
    public ClassName getClassName() {
        return ClassName.PropertyKey;
    }
}
