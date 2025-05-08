package com.alibaba.jvm.sandbox.repater.plugin.jservlet;

import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationListener;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.AbstractInvokePluginAdapter;
import com.alibaba.jvm.sandbox.repeater.plugin.core.model.EnhanceModel;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.InvokeType;
import com.alibaba.jvm.sandbox.repeater.plugin.spi.InvokePlugin;
import com.google.common.collect.Lists;
import org.kohsuke.MetaInfServices;

import java.util.List;

/**
 * {@link } http入口流量类型插件
 * <p>
 *
 */
@MetaInfServices(InvokePlugin.class)
public class JakartaServletPlugin extends AbstractInvokePluginAdapter {

    @Override
    protected List<EnhanceModel> getEnhanceModels() {
        // 拦截javax.servlet.http.HttpServlet#service(HttpServletRequest req, HttpServletResponse resp)
        EnhanceModel.MethodPattern mp = EnhanceModel.MethodPattern.builder()
                .methodName("service")
                .parameterType(new String[]{"jakarta.servlet.http.HttpServletRequest", "jakarta.servlet.http.HttpServletResponse"})
                .build();
        EnhanceModel em = EnhanceModel.builder()
                .classPattern("jakarta.servlet.http.HttpServlet")
                .methodPatterns(new EnhanceModel.MethodPattern[]{mp})
//                .methodPatterns(EnhanceModel.MethodPattern.transform("service"))
                .watchTypes(Event.Type.BEFORE, Event.Type.RETURN, Event.Type.THROWS)
                .build();
        return Lists.newArrayList(em);
    }

    @Override
    protected InvocationProcessor getInvocationProcessor() {
//        jakarta.servlet.http.HttpServlet
//        jakarta.servlet.http.HttpServlet
        // return null cause we override getEventListener
        return null;
    }

    @Override
    protected EventListener getEventListener(InvocationListener listener) {
        return new HttpStandaloneListener(getType(), isEntrance(), listener, getInvocationProcessor());
    }

    @Override
    public InvokeType getType() {
        return InvokeType.JAKARTA_SERVLET;
    }

    @Override
    public String identity() {
        return "jakarta-servlet";
    }

    @Override
    public boolean isEntrance() {
        return true;
    }
}
