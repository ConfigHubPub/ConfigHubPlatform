package com.confighub.core.system;

import com.confighub.core.store.APersisted;
import com.google.gson.JsonObject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

/**
 * Entity for persisting configuration data for ConfigHub instances.
 */
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Getter
@Setter
@NamedQueries({
      @NamedQuery(name = "SysConfig.byGroup",
                  query = "SELECT c FROM SystemConfig c WHERE configGroup=:groupName")
})
public class SystemConfig
        extends APersisted
{
    public enum ConfigGroup
    {
        LDAP
    }

    @Id
    @GeneratedValue
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "config_key", nullable = false, unique = true)
    private String key;

    @Column(name = "value")
    private String value;

    @Enumerated(EnumType.STRING)
    @Column(name = "config_group", nullable = false)
    private ConfigGroup configGroup;

    @Column(name = "encrypted")
    private boolean encrypted = false;

    public JsonObject toJson()
    {
        JsonObject json = new JsonObject();
        json.addProperty("key", this.key);

        if (!this.encrypted)
            json.addProperty("value", this.value);

        json.addProperty("configGroup", this.configGroup.name());
        json.addProperty("encrypted", this.encrypted);

        return json;
    }

    @Override
    public ClassName getClassName()
    {
        return ClassName.Configuration;
    }
}
