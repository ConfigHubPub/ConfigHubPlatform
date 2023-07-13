package com.confighub.core.store.dialect;

import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;

public class PostgreSQLDialect extends org.hibernate.dialect.PostgreSQL95Dialect
{
    public PostgreSQLDialect()
    {
        super();
        this.registerFunction("regexp", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "(?1 ~ ?2)::int"));
    }
}
