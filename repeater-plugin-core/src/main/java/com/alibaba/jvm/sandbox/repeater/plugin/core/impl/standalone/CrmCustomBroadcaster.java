package com.alibaba.jvm.sandbox.repeater.plugin.core.impl.standalone;

import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.AbstractBroadcaster;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api.DefaultBroadcaster;
import com.alibaba.jvm.sandbox.repeater.plugin.core.serialize.SerializeException;
import com.alibaba.jvm.sandbox.repeater.plugin.core.util.PathUtils;
import com.alibaba.jvm.sandbox.repeater.plugin.core.wrapper.RecordWrapper;
import com.alibaba.jvm.sandbox.repeater.plugin.core.wrapper.SerializerWrapper;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.Invocation;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.RecordModel;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.RepeatMeta;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.RepeatModel;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.RepeaterResult;
import com.google.gson.Gson;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class CrmCustomBroadcaster extends AbstractBroadcaster {

    private String recordSuffix = "record";

    private String repeatSuffix = "repeat";
    private Gson gson = new Gson();


    @Override
    protected void broadcastRecord(RecordModel rm) {
        try {
            String body = SerializerWrapper.jsonSerialize(rm);
            broadcast(body, rm.getTraceId(), recordSuffix);
        } catch (SerializeException e) {
            log.error("broadcast record failed", e);
        } catch (Throwable throwable) {
            log.error("[Error-0000]-broadcast record failed", throwable);
        }
    }

    @Override
    protected void broadcastRepeat(RepeatModel rm) {
        //不用实现
    }

    @Override
    public RepeaterResult<RecordModel> pullRecord(RepeatMeta meta) {
        ClassLoader swap = Thread.currentThread().getContextClassLoader();
        try {
            String record =
                    FileUtils.readFileToString(new File(assembleFileName(meta.getTraceId(), recordSuffix)), "UTF-8");
            Thread.currentThread().setContextClassLoader(DefaultBroadcaster.class.getClassLoader());
            RecordWrapper wrapper = SerializerWrapper.jsonDeserialize(record, RecordWrapper.class);
            if (meta.isMock() && CollectionUtils.isNotEmpty(wrapper.getSubInvocations())) {
                for (Invocation invocation : wrapper.getSubInvocations()) {
                    SerializerWrapper.inTimeDeserialize(invocation);
                }
            }
            SerializerWrapper.inTimeDeserialize(wrapper.getEntranceInvocation());
            final RepeaterResult<RecordModel> rm =
                    RepeaterResult.builder().success(true).message("operate success").data(wrapper.reTransform())
                            .build();
            log.debug("pullRecord success,traceId={},resp={}", meta.getTraceId(), gson.toJson(rm));
            return rm;
        } catch (Throwable e) {
            log.error("pullRecord failed", e);
            return RepeaterResult.builder().success(false).message(e.getMessage()).build();
        } finally {
            Thread.currentThread().setContextClassLoader(swap);
        }
    }

    private String getRemoteRecord(String appName, String traceId) {
        //假装从远程拉取录制
        return "";
    }

    private void broadcast(String body, String name, String folder) throws IOException {
        //打本地日志
        log.info("record|{}", body);
    }


    private String assembleFileName(String name, String folder) {
        return PathUtils.getModulePath() + File.separator + "repeater-data" + File.separator + folder + File.separator +
                name;
    }

}
