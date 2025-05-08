package com.alibaba.jvm.sandbox.repeater.plugin.ckv;

import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.AbstractInvokePluginAdapter;
import com.alibaba.jvm.sandbox.repeater.plugin.core.model.EnhanceModel;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.InvokeType;
import com.alibaba.jvm.sandbox.repeater.plugin.spi.InvokePlugin;
import com.google.common.collect.Lists;

import org.kohsuke.MetaInfServices;

import java.util.List;


/**
 * {@link CkvPlugin} jedis的java插件
 * <p>
 * 拦截{@code redis.clients.jedis.commands}包下面的commands实现类
 * <p>
 * 获取redis常用操作指令，不包括所有命令
 * 详见Jedis类、BinaryJedis类的实现接口
 * </p>
 */
@MetaInfServices(InvokePlugin.class)
public class CkvPlugin extends AbstractInvokePluginAdapter {

    @Override
    protected List<EnhanceModel> getEnhanceModels() {
        EnhanceModel.MethodPattern set = EnhanceModel.MethodPattern.builder()
                .methodName("set")
                .parameterType(new String[]{"byte[]", "byte[]"})
                .build();
        EnhanceModel.MethodPattern get = EnhanceModel.MethodPattern.builder()
                .methodName("get")
                .parameterType(new String[]{"byte[]"})
                .build();
        EnhanceModel jedis = EnhanceModel.builder()
                .classPattern("redis.clients.jedis.Jedis")
                .methodPatterns(new EnhanceModel.MethodPattern[]{set, get})
                .watchTypes(Event.Type.BEFORE, Event.Type.RETURN, Event.Type.THROWS)
                .build();
        return Lists.newArrayList(jedis);
    }


    @Override
    protected InvocationProcessor getInvocationProcessor() {
        return new CkvProcessor(getType());
    }

    @Override
    public InvokeType getType() {
        return InvokeType.CKV;
    }

    @Override
    public String identity() {
        return "ckv";
    }

    @Override
    public boolean isEntrance() {
        return false;
    }

}
