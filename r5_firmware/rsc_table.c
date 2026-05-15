/*
 * Copyright (c) 2014, Mentor Graphics Corporation
 * All rights reserved.
 * Copyright (c) 2021-2022 Xilinx, Inc. and Contributors. All rights reserved.
 * Copyright (c) 2022-2024 Advanced Micro Devices, Inc. All Rights Reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

/*
 * This file populates resource table for BM remote
 * for use by the Linux host
 */

#include <openamp/open_amp.h>
#include "rsc_table.h"
#include <string.h>

/* Place resource table in special ELF section */
#define __section_t(S)          __attribute__((__section__(#S)))
#define __resource              __attribute__((__section__(".resource_table"), __aligned__(4)))

#define RPMSG_VDEV_DFEATURES        (1 << VIRTIO_RPMSG_F_NS)

/* VirtIO rpmsg device id */
#define VIRTIO_ID_RPMSG_             7

#define NUM_VRINGS                  0x02
#define VRING_ALIGN                 0x1000
#define RING_TX                     0x3ED40000U
#define RING_RX                     0x3ED44000U
#define VRING_SIZE                  256

#define NUM_TABLE_ENTRIES           7  /* vdev + 2 vring + trace + 4 tensor blocks + rpu_ddr */
/* Trace buffer for the rsc_trace entry */
#if !defined(RSC_TRACE_SZ)
#define RSC_TRACE_SZ (4*1024)
#endif /* RSC_TRACE_SZ */
static char rsc_trace_buf[RSC_TRACE_SZ];

struct remote_resource_table __resource resources = {
	.version = 1,
	.num = NUM_TABLE_ENTRIES,
	.reserved = {0, 0},
	.offset[0] = offsetof(struct remote_resource_table, rpmsg_vdev),
	.offset[1] = offsetof(struct remote_resource_table, rsc_trace),
	.offset[2] = offsetof(struct remote_resource_table, tensor_block0),
	.offset[3] = offsetof(struct remote_resource_table, tensor_block1),
	.offset[4] = offsetof(struct remote_resource_table, tensor_block2),
	.offset[5] = offsetof(struct remote_resource_table, tensor_block3),
	.offset[6] = offsetof(struct remote_resource_table, rpu_ddr),
	/* Virtio device entry */
	.rpmsg_vdev = {
		.type =		RSC_VDEV,
		.id =		VIRTIO_ID_RPMSG_,
		.notifyid =	31,
		.dfeatures =	RPMSG_VDEV_DFEATURES,
		.gfeatures =	0,
		.config_len =	0,
		.status =	0,
		.num_of_vrings = NUM_VRINGS,
		.reserved =	{0, 0},
	},
	/* Vring rsc entry - part of vdev rsc entry */
	.rpmsg_vring0 = {RING_TX, VRING_ALIGN, VRING_SIZE, 1, 0},
	.rpmsg_vring1 = {RING_RX, VRING_ALIGN, VRING_SIZE, 2, 0},
	/* trace buffer for logs, accessible via debugfs */
	.rsc_trace = {
		.type =		RSC_TRACE,
		.da =		(uint32_t)rsc_trace_buf,
		.len =		sizeof(rsc_trace_buf),
		.reserved =	0,
		.name =		"r5_trace",
	},
	/*
	 * Tensor Memory Pool - 4个独立的10MB buffer用于Ping-Pong操作
	 *
	 * 对应设备树reserved-memory配置:
	 * reserved-memory {
	 *     #address-cells = <2>;
	 *     #size-cells = <2>;
	 *     ranges;
	 *
	 *     tensor_block0@3f100000 {
	 *         reg = <0x0 0x3f100000 0x0 0x00a00000>;  // 10MB
	 *         no-map;
	 *     };
	 *     tensor_block1@3fb00000 {
	 *         reg = <0x0 0x3fb00000 0x0 0x00a00000>;  // 10MB
	 *         no-map;
	 *     };
	 *     tensor_block2@40500000 {
	 *         reg = <0x0 0x40500000 0x0 0x00a00000>;  // 10MB
	 *         no-map;
	 *     };
	 *     tensor_block3@40f00000 {
	 *         reg = <0x0 0x40f00000 0x0 0x00a00000>;  // 10MB
	 *         no-map;
	 *     };
	 * };
	 */
	.tensor_block0 = {
		.type = RSC_CARVEOUT,
		.da = TENSOR_BLOCK_0_PA,
		.pa = TENSOR_BLOCK_0_PA,
		.len = TENSOR_BLOCK_SIZE,
		.flags = 0,
		.reserved = {0},
		.name = "tensor_block0",
	},
	.tensor_block1 = {
		.type = RSC_CARVEOUT,
		.da = TENSOR_BLOCK_1_PA,
		.pa = TENSOR_BLOCK_1_PA,
		.len = TENSOR_BLOCK_SIZE,
		.flags = 0,
		.reserved = {0},
		.name = "tensor_block1",
	},
	.tensor_block2 = {
		.type = RSC_CARVEOUT,
		.da = TENSOR_BLOCK_2_PA,
		.pa = TENSOR_BLOCK_2_PA,
		.len = TENSOR_BLOCK_SIZE,
		.flags = 0,
		.reserved = {0},
		.name = "tensor_block2",
	},
	.tensor_block3 = {
		.type = RSC_CARVEOUT,
		.da = TENSOR_BLOCK_3_PA,
		.pa = TENSOR_BLOCK_3_PA,
		.len = TENSOR_BLOCK_SIZE,
		.flags = 0,
		.reserved = {0},
		.name = "tensor_block3",
	},
	/*
	 * R5 私有 DDR - 存放固件代码和数据段
	 *
	 * 包含段：
	 * - .text, .init, .fini (代码)
	 * - .eh_frame, .eh_framehdr (异常处理)
	 * - .gcc_except_table, .ARM.exidx (C++异常)
	 * - .bss (未初始化数据)
	 *
	 * 对应设备树reserved-memory配置:
	 * reserved-memory {
	 *     rpu0_ddr_reserved: rpu0ddr@41900000 {
	 *         no-map;
	 *         reg = <0x0 0x41900000 0x0 0x00800000>;  // 8MB
	 *     };
	 * };
	 */
	.rpu_ddr = {
		.type = RSC_CARVEOUT,
		.da = RPU_DDR_PA,
		.pa = RPU_DDR_PA,
		.len = RPU_DDR_SIZE,
		.flags = 0,
		.reserved = {0},
		.name = "rpu_ddr",
	},
};

char *get_rsc_trace_info(uint32_t *len)
{
	*len = sizeof(rsc_trace_buf);
	return rsc_trace_buf;
}

void *get_resource_table (uint32_t rsc_id, uint32_t *len)
{
	(void) rsc_id;
	*len = sizeof(resources);
	return &resources;
}
