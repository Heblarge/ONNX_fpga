/*
 * Copyright (c) 2014, Mentor Graphics Corporation
 * All rights reserved.
 *
 * Copyright (c) 2021-2022 Xilinx, Inc. and Contributors. All rights reserved.
 * Copyright (c) 2022-2024 Advanced Micro Devices, Inc. All Rights Reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

/*
 * This file populates resource table for BM remote
 * for use by the Linux host
 */

#ifndef RSC_TABLE_H_
#define RSC_TABLE_H_

#include <stddef.h>
#include <openamp/open_amp.h>

#if defined __cplusplus
extern "C" {
#endif

#define NO_RESOURCE_ENTRIES         6  /* vdev + 2 vring + trace + 4 tensor blocks */

/* Tensor Memory Pool Configuration - 统一大小池，每块10MB */
#define TENSOR_BLOCK_SIZE           0x00A00000UL    /* 10MB per block */
#define TENSOR_BLOCK_0_PA           0x3F100000UL    /* Block 0: 0x3F100000 */
#define TENSOR_BLOCK_1_PA           0x3FB00000UL    /* Block 1: 0x3FB00000 */
#define TENSOR_BLOCK_2_PA           0x40500000UL    /* Block 2: 0x40500000 */
#define TENSOR_BLOCK_3_PA           0x40F00000UL    /* Block 3: 0x40F00000 */
#define TENSOR_POOL_TOTAL_SIZE      0x02800000UL    /* Total: 40MB */

/* Resource table for the given remote */
struct remote_resource_table {
	uint32_t version;
	uint32_t num;
	uint32_t reserved[2];
	uint32_t offset[NO_RESOURCE_ENTRIES];
	/* rpmsg vdev entry */
	struct fw_rsc_vdev rpmsg_vdev;
	struct fw_rsc_vdev_vring rpmsg_vring0;
	struct fw_rsc_vdev_vring rpmsg_vring1;
	struct fw_rsc_trace rsc_trace;
	/* Tensor memory pool - 4个独立的10MB buffer */
	struct fw_rsc_carveout tensor_block0;
	struct fw_rsc_carveout tensor_block1;
	struct fw_rsc_carveout tensor_block2;
	struct fw_rsc_carveout tensor_block3;
}__attribute__((packed, aligned(4)));

void *get_resource_table (uint32_t rsc_id, uint32_t *len);
char *get_rsc_trace_info(uint32_t *len);

#if defined __cplusplus
}
#endif

#endif /* RSC_TABLE_H_ */
