package com.confighub.core.store.dialect;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;

import com.google.common.collect.ImmutableMap;

/**
 * ConfigHub supports multiple different database drivers. While JDBC/hibernate are meant to abstract
 * away the annoyance of having to deal with different database drivers in code, some features (like regex)
 * are not handled by the default dialets. This custom dialect resolver allows ConfigHub to use its own
 * extended dialects for each supported database driver.
 */
public class ExtendedDialectResolver implements DialectResolver
{
	private static final Logger log = LogManager.getFormatterLogger(ExtendedDialectResolver.class);

	public static final Map<String, Class<?>> DIALECTS = ImmutableMap.of
	(
		"H2",         com.confighub.core.store.dialect.H2Dialect.class,
		"MySQL",      com.confighub.core.store.dialect.MySQLDialect.class,
		"Oracle",     com.confighub.core.store.dialect.OracleDialect.class,
		"PostgreSQL", com.confighub.core.store.dialect.PostgreSQLDialect.class
	);

    @Override
	public Dialect resolveDialect(DialectResolutionInfo info)
	{
		log.info("Resolving dialect for database name [%s]", info.getDatabaseName());

		try
		{
			Class<?> dialectCls = DIALECTS.get(info.getDatabaseName());
			if (dialectCls != null)
			{
				return (Dialect) dialectCls.newInstance();
			}
		}
		catch (Exception e)
		{
		}

		log.warn("Failed to resolve dialect - unsupported database name [%s]", info.getDatabaseName());
		return null;
	}

}
