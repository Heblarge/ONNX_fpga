package org.forwarder.backend.impls.HWAccelerated;

import java.util.UUID;

import org.forwarder.Session;
import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedBackend;
import org.nd4j.linalg.api.memory.MemoryWorkspace;
import org.nd4j.linalg.api.memory.conf.WorkspaceConfiguration;
import org.nd4j.linalg.api.memory.enums.AllocationPolicy;
import org.nd4j.linalg.api.memory.enums.LocationPolicy;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

public class HWAcceleratedSession extends Session<INDArray> {

    public static final String ND4J_WORKSPACE_NAME_PREFIX = "forwarder-session-";

    public static org.forwarder.backend.impls.HWAccelerated.HWAcceleratedSession getSession() {
        Session<?> sess = TL_SESSION.get();
        if (sess == null)
            throw new RuntimeException("No forwarder session binded in this thread");

        if (org.forwarder.backend.impls.HWAccelerated.HWAcceleratedSession.class.isInstance(sess) == false)
            throw new java.lang.ClassCastException("Session class type not match");

        org.forwarder.backend.impls.HWAccelerated.HWAcceleratedSession HWAcceleratedSess = org.forwarder.backend.impls.HWAccelerated.HWAcceleratedSession.class.cast(sess);
        return HWAcceleratedSess;
    }

    private MemoryWorkspace workspace;

    public HWAcceleratedSession(HWAcceleratedBackend backend) {
        super(backend);

        this.workspace = Nd4j.getWorkspaceManager().getAndActivateWorkspace(
                WorkspaceConfiguration
                        .builder()
                        .initialSize(100*1024*1024)
                        .maxSize(1024*1024*1024)
                        .policyAllocation(AllocationPolicy.STRICT)
                        .policyLocation(LocationPolicy.RAM)
                        .build(),
                ND4J_WORKSPACE_NAME_PREFIX + UUID.randomUUID());
        if(backend!=null)
        {this.workspace.enableDebug(backend.getModel().getConfig().isDebug());}
    }

    public MemoryWorkspace getMemoryWorkspace() {
        return this.workspace;
    }

    @Override
    public void close() throws Exception {
        super.close();
        workspace.destroyWorkspace();
        workspace.close();
        workspace = null;
    }

}
