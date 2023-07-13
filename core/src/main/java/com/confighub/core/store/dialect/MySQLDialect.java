package com.confighub.core.store.dialect;

import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;

public class MySQLDialect extends org.hibernate.dialect.MySQL55Dialect
{
    public MySQLDialect()
    {
        super();
        this.registerFunction("regexp", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "?1 regexp ?2"));
    }
}
