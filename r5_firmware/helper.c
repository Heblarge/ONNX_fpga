/*
 * Copyright (c) 2014, Mentor Graphics Corporation
 * All rights reserved.
 *
 * Copyright (c) 2021 Xilinx, Inc. All rights reserved.
 * Copyright (c) 2022-2024 Advanced Micro Devices, Inc. All Rights Reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

#include <stdio.h>
#include <stdarg.h>
#include "xparameters.h"
#include "xil_exception.h"
#include "xil_printf.h"
#include "xscugic.h"
#include "xil_cache.h"
#include <metal/sys.h>
#include <metal/irq.h>
#include "platform_info.h"
#include "rsc_table.h"
#include "datamover_driver.h"

#define INTC_DEVICE_ID		XPAR_SCUGIC_0_DEVICE_ID

XScuGic xInterruptController;  // 改为全局，供 DataMover 驱动使用

/* Interrupt Controller setup */
static int32_t app_gic_initialize(void)
{
	uint32_t status;
	XScuGic_Config *int_ctrl_config; /* interrupt controller configuration params */
	uint32_t int_id;
	uint32_t mask_cpu_id = ((u32)0x1 << XPAR_CPU_ID);
	uint32_t target_cpu;

	mask_cpu_id |= mask_cpu_id << 8U;
	mask_cpu_id |= mask_cpu_id << 16U;

	Xil_ExceptionDisable();

	/*
	 * Initialize the interrupt controller driver
	 */
	int_ctrl_config = XScuGic_LookupConfig(INTC_DEVICE_ID);
	if (NULL == int_ctrl_config) {
		return XST_FAILURE;
	}

	status = XScuGic_CfgInitialize(&xInterruptController, int_ctrl_config,
				       int_ctrl_config->CpuBaseAddress);
	if (status != XST_SUCCESS) {
		return XST_FAILURE;
	}

	// 注意：不要修改中断目标寄存器，这会阻止其他中断到达 CPU
	// 裸机测试代码中没有这部分，所以我们移除它

	/*
	 * Register the interrupt handler to the hardware interrupt handling
	 * logic in the ARM processor.
	 */
	Xil_ExceptionRegisterHandler(XIL_EXCEPTION_ID_IRQ_INT,
			(Xil_ExceptionHandler)&XScuGic_InterruptHandler,
			&xInterruptController);

	/* Disable the interrupt before enabling exception to avoid interrupts
	 * received before exception is enabled.
	 */
	XScuGic_Disable(&xInterruptController, IPI_IRQ_VECT_ID);

	Xil_ExceptionEnable();

	/* Connect Interrupt ID with ISR */
	XScuGic_Connect(&xInterruptController, IPI_IRQ_VECT_ID,
			(Xil_ExceptionHandler)&metal_xlnx_irq_isr,
			(void *)IPI_IRQ_VECT_ID);

	/* Enable IPI interrupt for RPMsg communication */
	XScuGic_Enable(&xInterruptController, IPI_IRQ_VECT_ID);

	return 0;
}

/*
 * A circular buffer for libmetal log. Need locks if ported to MT world.
 * c_buf - pointer to the buffer referenced in the resource table
 * c_len - size of the buffer
 * c_pos - next rext record position
 * c_cnt - free running count of records to help sorting in case of overrun
 */
static struct {
	char * c_buf;
	uint32_t c_len;
	uint32_t c_pos;
	uint32_t c_cnt;
} circ;

static void rsc_trace_putchar(char c)
{
	if (circ.c_pos >= circ.c_len)
		circ.c_pos = 0;
	circ.c_buf[circ.c_pos++] = c;
}

static void rsc_trace_logger(enum metal_log_level level,
			   const char *format, ...)
{
	char msg[128];
	char *p;
	int32_t len;
	va_list args;

	/* prefix "cnt L6 ": record count and log level */
	len = sprintf(msg, "%u L%u ", circ.c_cnt, level);
	if (len < 0 || len >= sizeof(msg))
		len = 0;
	circ.c_cnt++;

	va_start(args, format);
	vsnprintf(msg + len, sizeof(msg) - len, format, args);
	va_end(args);

	/* copy at most sizeof(msg) to the circular buffer */
	for (len = 0, p = msg; *p && len < sizeof(msg); ++len, ++p)
		rsc_trace_putchar(*p);
	/* Remove this xil_printf to stop printing to console */
	xil_printf("%s", msg);
}

/* Main hw machinery initialization entry point, called from main()*/
/* return 0 on success */
int32_t init_system(void)
{
	int32_t ret;
	struct metal_init_params metal_param = METAL_INIT_DEFAULTS;

	circ.c_buf = get_rsc_trace_info(&circ.c_len);
	if (circ.c_buf && circ.c_len){
		metal_param.log_handler = &rsc_trace_logger;
		metal_param.log_level = METAL_LOG_DEBUG;
		circ.c_pos = circ.c_cnt = 0;
	};

	/* Low level abstraction layer for openamp initialization */
	metal_init(&metal_param);

	/* configure the global interrupt controller */
	app_gic_initialize();

	/* Initialize metal Xilinx IRQ controller */
	ret = metal_xlnx_irq_init();
	if (ret != 0) {
		ML_ERR("metal_xlnx_irq_init failed.\r\n");
	}

	ML_DBG("c_buf,c_len = %p,%u\r\n", circ.c_buf, circ.c_len);
	return ret;
}

void cleanup_system()
{
	metal_finish();

	Xil_DCacheDisable();
	Xil_ICacheDisable();
	Xil_DCacheInvalidate();
	Xil_ICacheInvalidate();
}
