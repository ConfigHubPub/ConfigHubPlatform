/*
 * This file is part of ConfigHub.
 *
 * ConfigHub is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ConfigHub is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ConfigHub.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.confighub.api.server.filters;

import com.confighub.core.utils.Utils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URL;

public class UrlRewriteFilter
        implements Filter
{
    private static final Logger log = LogManager.getLogger(UrlRewriteFilter.class);

    @Override
    public void init(FilterConfig filterConfig)
            throws ServletException
    { }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws ServletException, IOException
    {
        if (!(request instanceof HttpServletRequest))
        {
            chain.doFilter(request, response);
            return;
        }

        String url = ((HttpServletRequest)request).getRequestURL().toString().toLowerCase();
        URL aUrl = new URL(url);
        String path = aUrl.getPath();

        if (path.startsWith("/email-verification") ||
            path.startsWith("/passwordReset"))
        {
            request.getRequestDispatcher("/index.html").forward(request, response);
            return;
        }

        if (Utils.isBlank(path) || path.equals("/") || path.equals("/index.html") ||
            path.startsWith("/rest"))
        {
            try
            {
                chain.doFilter(request, response);
            }
            catch (Exception e)
            {
                request.getRequestDispatcher("/404.html").forward(request, response);
            }
            return;
        }

        if (path.startsWith("/r/") ||
            path.startsWith("/account/") ||
            path.contains("edit/file/"))
        {
            request.getRequestDispatcher("/index.html").forward(request, response);
            return;
        }

        boolean hasExt = Utils.isBlank(FilenameUtils.getExtension(url));

        if (!hasExt)
        {
            try
            {
                chain.doFilter(request, response);
            }
            catch (Exception e)
            {
                request.getRequestDispatcher("/404.html").forward(request, response);
            }
            return;
        }

        request.getRequestDispatcher("/index.html").forward(request, response);
    }

    @Override
    public void destroy() { }
}
