package com.confighub.core.store.dialect;

import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;

public class OracleDialect extends org.hibernate.dialect.Oracle10gDialect
{
    public OracleDialect()
    {
        super();
        this.registerFunction("regexp", new SQLFunctionTemplate(StandardBasicTypes.BOOLEAN, "(case when (regexp_like(?1, ?2)) then 1 else 0 end)"));
    }
}
