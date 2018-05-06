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
        return LdapConfig.builder()
                         .ldapEnabled(config.containsKey("ldapEnabled")
                                              ? Boolean.valueOf(config.get("ldapEnabled").getValue())
                                              : false)
                         .systemUsername(config.get("systemUsername").getValue())
                         .systemPassword(config.get("systemPassword").getValue())
                         .ldapUrl(config.get("ldapUrl").getValue())
                         .trustAllCertificates(config.containsKey("trustAllCertificates")
                                                       ? Boolean.valueOf(config.get("trustAllCertificates").getValue())
                                                       : false)
                         .activeDirectory(config.containsKey("activeDirectory")
                                                  ? Boolean.valueOf(config.get("activeDirectory").getValue())
                                                  : false)
                         .searchBase(config.get("searchBase").getValue())
                         .searchPattern(config.get("searchPattern").getValue())
                         .displayName(config.get("displayName").getValue())
                         .groupSearchBase(config.get("groupSearchBase").getValue())
                         .groupIdAttribute(config.get("groupIdAttribute").getValue())
                         .groupSearchPattern(config.get("groupSearchPattern").getValue())
                         .build();
    }

}
