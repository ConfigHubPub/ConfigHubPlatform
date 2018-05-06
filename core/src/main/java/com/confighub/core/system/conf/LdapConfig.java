package com.confighub.core.system.conf;

import com.confighub.core.system.SystemConfig;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LdapConfig
{
    private final static SystemConfig.ConfigGroup group = SystemConfig.ConfigGroup.LDAP;

    private boolean ldapEnabled;
    private String systemUsername;
    private String systemPassword;
    private String ldapUrl;
    private boolean trustAllCertificates;
    private boolean activeDirectory;
    private String searchBase;
    private String searchPattern;
    private String displayName;
    private String groupSearchBase;
    private String groupIdAttribute;
    private String groupSearchPattern;

    public static LdapConfig build(final Map<String, SystemConfig> config)
    {
        if (null == config)
            return new LdapConfig();

        return LdapConfig.builder()
                         .ldapEnabled(iorb(config, "ldapEnabled", false))
                         .systemUsername(ior(config, "systemUsername", null))
                         .systemPassword(ior(config, "systemPassword", null))
                         .ldapUrl(ior(config, "ldapUrl", null))
                         .trustAllCertificates(iorb(config,"trustAllCertificates", false))
                         .activeDirectory(iorb(config, "activeDirectory", false))
                         .searchBase(ior(config, "searchBase", null))
                         .searchPattern(ior(config, "searchPattern", null))
                         .displayName(ior(config, "displayName", null))
                         .groupSearchBase(ior(config, "groupSearchBase", null))
                         .groupIdAttribute(ior(config, "groupIdAttribute", null))
                         .groupSearchPattern(ior(config, "groupSearchPattern", null))
                         .build();
    }

    private static String ior(final Map<String, SystemConfig> config,
                             final String key,
                             final String de)
    {
        if (config.containsKey(key))
            return config.get(key).getValue();

        return de;
    }

    private static boolean iorb(final Map<String, SystemConfig> config,
                                final String key,
                                final boolean de)
    {
        if (config.containsKey(key))
            return Boolean.valueOf(config.get(key).getValue());

        return de;
    }

}
