package com.rackspace.papi.components.hnorm;

import com.rackspace.papi.filter.FilterConfigHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspacecloud.api.docs.repose.header_normalization.v1.HeaderNormalizationConfig;

import javax.servlet.*;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeaderNormalizationFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(HeaderNormalizationFilter.class);
    private static String DEFAULT_CONFIG = "header-normalization.cfg.xml";
    private String config;
    private HeaderNormalizationHandlerFactory handlerFactory;
    private ConfigurationService configurationManager;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
    }

    @Override
    public void destroy() {
        configurationManager.unsubscribeFrom(config, handlerFactory);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);
        LOG.info("Initializing filter using config " + config);
        configurationManager = ServletContextHelper.getInstance().getPowerApiContext(filterConfig.getServletContext()).configurationService();
        handlerFactory = new HeaderNormalizationHandlerFactory();
        configurationManager.subscribeTo(config, handlerFactory, HeaderNormalizationConfig.class);
    }
}
