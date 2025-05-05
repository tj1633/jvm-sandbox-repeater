package com.alibaba.jvm.sandbox.repeater.plugin.elasticsearch;

import com.alibaba.jvm.sandbox.api.event.Event.Type;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.AbstractInvokePluginAdapter;
import com.alibaba.jvm.sandbox.repeater.plugin.core.model.EnhanceModel;
import com.alibaba.jvm.sandbox.repeater.plugin.core.model.EnhanceModel.MethodPattern;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.InvokeType;
import com.alibaba.jvm.sandbox.repeater.plugin.spi.InvokePlugin;
import com.google.common.collect.Lists;
import org.kohsuke.MetaInfServices;

import java.util.List;

/**
 * <p>
 *
 * @author zhaoyb1990
 */
@MetaInfServices(InvokePlugin.class)
public class ElasticSearchPlugin extends AbstractInvokePluginAdapter {

  @Override
  protected List<EnhanceModel> getEnhanceModels() {
    MethodPattern performRequest = MethodPattern.builder()
        .methodName("performRequest")
        .parameterType(new String[]{"org.elasticsearch.client.Request"})
        .build();
    EnhanceModel em = EnhanceModel.builder()
        .classPattern("org.elasticsearch.client.RestClient")
        .methodPatterns(new MethodPattern[]{performRequest})
        .watchTypes(Type.BEFORE, Type.RETURN, Type.THROWS)
        .build();
    return Lists.newArrayList(em);
  }

  @Override
  protected InvocationProcessor getInvocationProcessor() {
    return new ElasticSearchProcessor(getType());
  }

  @Override
  public InvokeType getType() {
    return InvokeType.ELASTICSEARCH;
  }

  @Override
  public String identity() {
    return "elasticsearch";
  }

  @Override
  public boolean isEntrance() {
    return false;
  }

}
