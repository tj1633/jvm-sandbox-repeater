package com.alibaba.jvm.sandbox.repeater.plugin.ckv;

import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.event.ReturnEvent;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api.DefaultInvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.core.util.LogUtil;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.Identity;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.Invocation;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.InvokeType;
import com.google.common.io.BaseEncoding;
import org.apache.commons.collections4.MapUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

class CkvProcessor extends DefaultInvocationProcessor {

    CkvProcessor(InvokeType type) {
        super(type);
    }


//    @Override
//    public Object[] assembleRequest(BeforeEvent event) {
//        try {
//            //TODO byte[]手动序列化一下算了，不依赖fastjson了
//            Map<String, Object> params = new HashMap<String, Object>();
//            if (event.argumentArray.length > 0) {
//                for (int i = 0; i < event.argumentArray.length; i++) {
//                    params.put("param" + i, event.argumentArray[i]);
//                }
//            }
//            return new Object[]{params};
//        } catch (Exception e) {
//            LogUtil.error("ckv-plugin assembleRequest error, event={}",
//                    event.javaClassName + "|" + event.javaMethodName, e);
//        }
//        return new Object[]{};
//    }

//    @Override
//    public Object assembleResponse(Event event) {
//        // assembleResponse可能在before事件中被调用，这里只需要在return时间中获取返回值
//        if (event.type == Event.Type.RETURN) {
//            ReturnEvent returnEvent = (ReturnEvent) event;
//            // 获取返回值
//            Map<String, Object> responseMap = new HashMap<String, Object>();
//            Object responseRaw = returnEvent.object;
//            if (responseRaw == null) {
//                return responseMap;
//            }
//            responseMap.put("responseBody", responseRaw);
//            try {
//                return responseMap;
//            } catch (Exception e) {
//                LogUtil.error("ckv-plugin copy response error", e);
//            }
//        }
//
//        return null;
//    }

    @Override
    public Object assembleMockResponse(BeforeEvent event, Invocation invocation) {
        try {
            if (invocation.getResponse() == null) {
                return null;
            }
            final String response = (String) invocation.getResponse();
            final byte[] decoded = BaseEncoding.base64().decode(response);
            return decoded;
        } catch (Exception e) {
            LogUtil.error("ckv-plugin assembleMockResponse error, event={}",
                    event.javaClassName + "|" + event.javaMethodName, e);
        }
        return null;
    }
//    @Override
//    public Object assembleMockResponse(BeforeEvent event, Invocation invocation) {
//        try {
//            // 构建返回body体
//            Map<String, Object> responseMap = (Map<String, Object>) invocation.getResponse();
//            if (MapUtils.isEmpty(responseMap)) {
//                new Object();
//            }
//            String responseRaw = (String)responseMap.get("responseBody");
//            LogUtil.info("ckv-plugin responseRaw={}", responseRaw.getClass().getName());
//
//            return responseRaw;
//        } catch (Exception e) {
//            LogUtil.error("ckv-plugin assembleMockResponse error, event={}",
//                    event.javaClassName + "|" + event.javaMethodName, e);
//        }
//        return null;
//    }

}
