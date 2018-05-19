package com.confighub.core.system.conf;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class LdapTestConfig
        extends LdapConfig
{
    private boolean testConnectionOnly;
    private String principal;
    private String password;
}
