package com.confighub.core.store.dialect;

import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;

public class H2Dialect extends org.hibernate.dialect.H2Dialect
{
    public H2Dialect()
    {
        super();
        this.registerFunction("regexp", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "regexp_like(?1, replace(replace(?2, '{', '\\{'), '}', '\\}'))"));
    }
}
