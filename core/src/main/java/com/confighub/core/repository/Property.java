/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.core.repository;

import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.resolver.Context;
import com.confighub.core.store.diff.PropertyDiffTracker;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.*;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Table(
        indexes = {@Index(name = "PROP_repoIndex", columnList = "id, propertyKey_id, repositoryId")}
)
@NamedQueries({
    @NamedQuery(name = "Property.get",
                query = "SELECT p FROM Property p WHERE repository=:repository AND id=:id"),

    @NamedQuery(name = "Search.values",
                query = "SELECT p FROM Property p WHERE " +
                    "p.repository=:repository AND UPPER(p.value) LIKE :searchTerm"),

    @NamedQuery(name = "Property.count", query = "SELECT COUNT(c) FROM Property c WHERE c.repository.active=true"),
})
@Audited
@EntityListeners({PropertyDiffTracker.class})
public class Property
        extends AContextAwarePersistent
{
    private static final Logger log = LogManager.getLogger(Property.class);

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.REFRESH })
    @JoinColumn(nullable = false)
    private PropertyKey propertyKey;

    @ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.REFRESH, CascadeType.PERSIST })
    @JoinColumn(name = "absoluteFilePath")
    private AbsoluteFilePath absoluteFilePath;

    @Column(length = 10485760)
    private String value;

    // --------------------------------------------------------------------------------------------
    // Construction
    // --------------------------------------------------------------------------------------------

    protected Property() {}

    public Property(final Repository repository)
    {
        this.repository = repository;
    }

    // --------------------------------------------------------------------------------------------
    // Validation before saving
    // --------------------------------------------------------------------------------------------
    @PreRemove
    public void preRemove()
    {
        this.propertyKey.removeProperty(this);
    }

    private transient boolean enforced = false;

    /**
     * Rules for a correct property
     *
     * 1. Key cannot be blank;
     * 2. Context cannot contain more than one level with the same depth
     * 3. Property with the same context signature
     *
     * @throws ConfigException
     */
    @PostUpdate
    @PostPersist
    public void enforce()
            throws ConfigException
    {
        if (enforced)
            return;

        // Inactive properties can exist without any restrictions
        if (!this.active)
            return;

        // 1. Key cannot be blank;
        if (null == propertyKey)
            throw new ConfigException(Error.Code.KEY_BLANK);

        // 2. Context cannot contain more than one level with the same depth
        validateContext();


        // 3. Property with the same context signature
        for (Property property : propertyKey.getProperties())
        {
            if (property.equals(this) || !property.isActive())
                continue;

            if (property.getContextWeight() == this.getContextWeight())
            {
                if (CollectionUtils.isEqualCollection(this.context, property.getContext()))
                {
                    throw new ConfigException(Error.Code.PROP_DUPLICATION_CONTEXT);
                }
            }
        }

        // 4. Cross-cluster conflict detection
        if (this.repository.isContextClustersEnabled() &&
            null != this.context &&
            this.propertyKey.getProperties().size() > 1)
        {

            // Check if the key for this property has values with the same context weight, as this is
            // the first indicator of a potential cross-cluster conflict.
            List<AContextAwarePersistent> conflictCandidates = null;
            for (Property property : this.propertyKey.getProperties())
            {
                if (property.equals(this))
                     continue;

                if (this.contextWeight == property.getContextWeight())
                {
                    if (null == conflictCandidates)
                        conflictCandidates = new ArrayList<>();
                    conflictCandidates.add(property);
                }
            }

            crossClusterValidation(conflictCandidates);
        }

        if (null != this.absoluteFilePath &&
            this.active &&
            PropertyKey.ValueDataType.FileEmbed.equals(this.propertyKey.getValueDataType()))
        {
            Context context = new Context(null, this.repository, this.getContext(), null);

            HashMap<RepoFile, Property> breadcrumbs = new LinkedHashMap<>();
            checkPropertyCircularReference(context, this, breadcrumbs);
        }

        enforced = true;
    }


    // --------------------------------------------------------------------------------------------
    // PropertyKey management
    // --------------------------------------------------------------------------------------------

    public String getKey()
    {
        return this.propertyKey.getKey();
    }

    public String getReadme() { return this.propertyKey.getReadme(); }

    public Set<Property> getKeyProperties() { return this.propertyKey.getProperties(); }

    public Long getPropertyKeyId() { return this.propertyKey.getId(); }

    // --------------------------------------------------------------------------------------------
    // POJO Ops
    // --------------------------------------------------------------------------------------------

    @Override
    public String toString()
    {
        return String.format("[%s] Property[%5d]: %s | key: %s | value: %s | context[%3d]: %s | %s",
                             this.revType,
                             this.id,
                             this.isEditable ? "rw" : "ro",
                             this.getKey(),
                             this.getValue(),
                             this.getContextWeight(),
                             getContextJson(),
                             null ==  this.type ? "No type" : this.type.name());
    }

    @Override
    public int hashCode()
    {
        // Because envers will store deleted item without any data, we have to be ready for this.
        if (Utils.anyNull(repository, propertyKey))
            return this.id.intValue();

        return Objects.hash(this.repository.getName(), this.propertyKey.getKey(), this.contextWeight, this.getValue());
    }

    @Override
    public boolean equals(Object o)
    {
        if (null == o || !(o instanceof Property)) return false;
        Property other = (Property)o;

        return
                Utils.same(this.getContextJson(), other.getContextJson()) &&
                Utils.same(this.getValue(), other.getValue()) &&
                this.isActive() == other.isActive() &&
                Utils.equal(this.id, other.id);
    }

    public JsonObject toJson()
    {
        JsonObject json = new JsonObject();
        json.addProperty("id", this.getId());
        json.addProperty("vdt", this.getPropertyKey().getValueDataType().name());

        Gson gson = new Gson();
        switch (this.getPropertyKey().getValueDataType())
        {
            case Map:
                json.add("value", gson.fromJson(this.getValue(), JsonObject.class));
                break;

            case List:
                json.add("value", gson.fromJson(this.getValue(), JsonArray.class));
                break;

            default:
                json.addProperty("value", this.getValue());
        }

        json.add("levels", gson.fromJson(this.contextJson, JsonArray.class));

        return json;
    }

    // --------------------------------------------------------------------------------------------
    // Setters and getters
    // --------------------------------------------------------------------------------------------

    public Long getId()
    {
        return id;
    }

    public PropertyKey getPropertyKey()
    {
        return this.propertyKey;
    }

    public void setReadme(final String readme)
    {
        this.propertyKey.setReadme(readme);
    }

    /**
     * Set new propertyKey, and disassociate from the old one.
     *
     * @param propertyKey
     */
    public void setPropertyKey(PropertyKey propertyKey)
    {
        if (null == propertyKey)
            return;

        if (null != this.propertyKey)
        {
            if (propertyKey.equals(this.propertyKey))
                return;

            this.propertyKey.removeProperty(this);
        }

        this.propertyKey = propertyKey;
        this.propertyKey.addProperty(this);
    }


    public String getValue()
    {
        if (null != this.absoluteFilePath)
        {
            return this.absoluteFilePath.getAbsPath();
        }
        return value;
    }

    public void setValue(String value)
        throws ConfigException
    {
        setValue(value, null);
    }

    public void decryptValue(final String encryptionSecret)
            throws ConfigException
    {
        if (!this.isEncrypted() || decrypted) return;
        if (null == this.absoluteFilePath)
            this.value = this.getPropertyKey().getSecurityProfile().decrypt(this.getValue(), encryptionSecret);

        this.decrypted = true;
    }

    public void encryptValue(final String encryptionSecret)
            throws ConfigException
    {
        if (!this.isEncrypted()) return;

        if (null == this.absoluteFilePath)
            this.value = this.getPropertyKey().getSecurityProfile().encrypt(this.getValue(), encryptionSecret);
    }

    public AbsoluteFilePath getAbsoluteFilePath()
    {
        return absoluteFilePath;
    }

    public void setAbsoluteFilePath(AbsoluteFilePath absoluteFilePath)
    {
        this.absoluteFilePath = absoluteFilePath;
        this.value = null;
    }

    public void setValue(String value, String encryptionSecret)
            throws ConfigException
    {
        if (repository.isValueTypeEnabled())
        {
            PropertyKey.ValueDataType vdt = this.getPropertyKey().getValueDataType();
            switch (vdt)
            {
                case Text:
                case Code:
                case FileRef:
                case FileEmbed:
                    break; // anything goes.

                case JSON:
                    if (null != value)
                        Utils.isJSONValid(value);
                    break;

                case Boolean:
                    if (null != value && !"true".equals(value) && !"false".equals(value))
                        throw new ConfigException(Error.Code.INVALID_VALUE_FOR_DATA_TYPE);
                    break;

                case Integer:
                    if (null == value) break;

                    try
                    {
                        Integer.valueOf(value);
                    }
                    catch (Exception ignore)
                    {
                        throw new ConfigException(Error.Code.INVALID_VALUE_FOR_DATA_TYPE);
                    }
                    break;

                case Long:
                    if (null == value) break;
                    try
                    {
                        Long.valueOf(value);
                    }
                    catch (Exception ignore)
                    {
                        throw new ConfigException(Error.Code.INVALID_VALUE_FOR_DATA_TYPE);
                    }
                    break;

                case Double:
                    if (null == value) break;
                    try
                    {
                        Double.valueOf(value);
                        if (value.startsWith("."))
                            value = "0" + value;
                    }
                    catch (Exception ignore)
                    {
                        throw new ConfigException(Error.Code.INVALID_VALUE_FOR_DATA_TYPE);
                    }
                    break;

                case Float:
                    if (null == value) break;
                    try
                    {
                        Float.valueOf(value);
                        if (value.startsWith("."))
                            value = "0" + value;
                    }
                    catch (Exception ignore)
                    {
                        throw new ConfigException(Error.Code.INVALID_VALUE_FOR_DATA_TYPE);
                    }
                    break;

                case Map:
                    if (null == value) break;
                    try
                    {
                        if (Utils.isBlank(value))
                            value = "{}";
                        else
                        {
                            new Gson().fromJson(value, JsonObject.class);
                        }
                    }
                    catch (Exception ignore)
                    {
                        throw new ConfigException(Error.Code.INVALID_VALUE_FOR_DATA_TYPE);
                    }
                    break;

                case List:
                    if (null == value) break;
                    try
                    {
                        if (Utils.isBlank(value))
                            value = "[]";
                        else
                            new Gson().fromJson(value, JsonArray.class);
                    }
                    catch (Exception ignore)
                    {
                        throw new ConfigException(Error.Code.INVALID_VALUE_FOR_DATA_TYPE);
                    }
                    break;
            }
        }

        this.absoluteFilePath = null;

        if (!this.isSecure())
        {
            this.value = value;
            return;
        }

        if (this.repository.isSecurityProfilesEnabled())
        {
            if (!this.getPropertyKey().getSecurityProfile().isSecretValid(encryptionSecret))
                throw new ConfigException(Error.Code.INVALID_PASSWORD);

            if (this.isEncrypted())
                this.value = this.getPropertyKey().getSecurityProfile().encrypt(value, encryptionSecret);
            else
                this.value = value;
        }
        else
            this.value = value;
    }

    private transient boolean decrypted = false;
    public boolean isEncrypted()
    {
        return this.propertyKey.isEncrypted();
    }

    public boolean isSecure() { return this.propertyKey.isSecure(); }

    @Override
    public ClassName getClassName() {
        return ClassName.Property;
    }
}
