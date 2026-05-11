package org.forwarder.backend.impls.HWAccelerated;

import org.forwarder.Session;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 硬件加速器会话
 *
 * 使用INDArray接口，内部数据在共享内存中
 */
public class HWAcceleratedSession extends Session<INDArray> {

	private static Logger logger = LoggerFactory.getLogger(HWAcceleratedSession.class);

	public static HWAcceleratedSession getSession() {
		Session<?> sess = TL_SESSION.get();
		if (sess == null) {
			throw new RuntimeException("No forwarder session binded in this thread");
		}

		if (!HWAcceleratedSession.class.isInstance(sess)) {
			throw new ClassCastException("Session class type not match, expected HWAcceleratedSession");
		}

		return HWAcceleratedSession.class.cast(sess);
	}

	private final HWAcceleratedBackend backend;

	public HWAcceleratedSession(HWAcceleratedBackend backend) {
		super(backend);
		this.backend = backend;
		logger.debug("Created HWAcceleratedSession");
	}

	@Override
	public HWAcceleratedBackend getBackend() {
		return backend;
	}
}
