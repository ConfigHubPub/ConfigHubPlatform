package com.confighub.core.system.conf;

import com.confighub.core.auth.TrustAllX509TrustManager;
import com.confighub.core.system.SystemConfig;
import lombok.*;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;

import java.net.URI;
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

    @Builder.Default
    private boolean ldapEnabled = false;

    @Builder.Default
    private boolean localAccountsEnabled = true;

    private String systemUsername;

    private String systemPassword;

    private String ldapUrl;

    private boolean trustAllCertificates;

    private boolean activeDirectory;

    private String searchBase;

    private String searchPattern;

    private String nameAttribute;

    private String emailAttribute;

    private String groupSearchBase;

    private String groupIdAttribute;

    private String groupSearchPattern;


    public LdapConnectionConfig toConnectionConfig()
    {
        final LdapConnectionConfig config = new LdapConnectionConfig();
        final URI ldapUri = URI.create( this.getLdapUrl() );
        config.setLdapHost( ldapUri.getHost() );
        config.setLdapPort( ldapUri.getPort() );
        config.setUseSsl( ldapUri.getScheme().startsWith( "ldaps" ) );
        config.setUseTls( false );

        if ( this.isTrustAllCertificates() )
        {
            config.setTrustManagers( new TrustAllX509TrustManager() );
        }

        config.setName( this.getSystemUsername() );
        config.setCredentials( this.getSystemPassword() );

        return config;
    }


    public static LdapConfig build( final Map<String, SystemConfig> config )
    {
        if ( null == config )
        {
            return new LdapConfig();
        }

        return LdapConfig.builder()
                         .ldapEnabled( iorb( config, "ldapEnabled", false ) )
                         .localAccountsEnabled( iorb( config, "localAccountsEnabled", true ) )
                         .systemUsername( ior( config, "systemUsername", null ) )
                         .systemPassword( ior( config, "systemPassword", null ) )
                         .ldapUrl( ior( config, "ldapUrl", null ) )
                         .trustAllCertificates( iorb( config, "trustAllCertificates", false ) )
                         .activeDirectory( iorb( config, "activeDirectory", false ) )
                         .searchBase( ior( config, "searchBase", null ) )
                         .searchPattern( ior( config, "searchPattern", null ) )
                         .nameAttribute( ior( config, "nameAttribute", null ) )
                         .emailAttribute( ior( config, "emailAttribute", null ) )
                         .groupSearchBase( ior( config, "groupSearchBase", null ) )
                         .groupIdAttribute( ior( config, "groupIdAttribute", null ) )
                         .groupSearchPattern( ior( config, "groupSearchPattern", null ) )
                         .build();
    }


    private static String ior( final Map<String, SystemConfig> config,
                               final String key,
                               final String de )
    {
        if ( config.containsKey( key ) )
        {
            return config.get( key ).getValue();
        }

        return de;
    }


    private static boolean iorb( final Map<String, SystemConfig> config,
                                 final String key,
                                 final boolean de )
    {
        if ( config.containsKey( key ) )
        {
            return Boolean.valueOf( config.get( key ).getValue() );
        }

        return de;
    }
}
