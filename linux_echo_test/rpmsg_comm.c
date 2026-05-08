/*
 * rpmsg_comm.c
 *
 * RPMsg communication wrapper implementation
 */

#include "rpmsg_comm.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/ioctl.h>
#include <linux/rpmsg.h>
#include <poll.h>
#include <dirent.h>
#include <sys/stat.h>

// RPMsg 句柄结构
struct rpmsg_handle {
    int ctrl_fd;       // 控制设备文件描述符
    int ept_fd;        // 端点设备文件描述符
    char ept_path[256]; // 端点设备路径
    char ept_name[64];  // 端点名称
};

// 从 echotest.c 复制的辅助函数
static char* get_rpmsg_ept_dev_name(const char* rpmsg_char_name,
                                     const char* ept_name,
                                     char* ept_dev_name) {
    char sys_rpmsg_ept_name_path[128];
    char svc_name[64];
    char *sys_rpmsg_path = "/sys/class/rpmsg";
    FILE *fp;
    int i;
    int ept_name_len;

    for (i = 0; i < 128; i++) {
        sprintf(sys_rpmsg_ept_name_path, "%s/%s/rpmsg%d/name",
                sys_rpmsg_path, rpmsg_char_name, i);
        if (access(sys_rpmsg_ept_name_path, F_OK) < 0)
            continue;
        fp = fopen(sys_rpmsg_ept_name_path, "r");
        if (!fp) {
            continue;
        }
        fgets(svc_name, sizeof(svc_name), fp);
        fclose(fp);
        ept_name_len = strlen(ept_name);
        if (ept_name_len > sizeof(svc_name))
            ept_name_len = sizeof(svc_name);
        if (!strncmp(svc_name, ept_name, ept_name_len)) {
            sprintf(ept_dev_name, "rpmsg%d", i);
            return ept_dev_name;
        }
    }

    printf("Not able to find RPMsg endpoint file for %s\n", ept_name);
    return NULL;
}

static int rpmsg_create_ept(int rpfd, struct rpmsg_endpoint_info *eptinfo) {
    int ret = ioctl(rpfd, RPMSG_CREATE_EPT_IOCTL, eptinfo);
    if (ret)
        perror("Failed to create endpoint");
    return ret;
}

rpmsg_handle_t* rpmsg_open(const char* device) {
    char fpath[256];
    char rpmsg_char_name[16];
    struct rpmsg_endpoint_info eptinfo;
    char ept_dev_name[16];
    char ept_dev_path[32];
    const char *rpmsg_dev = device;
    const char *rpmsg_ctrl_dev = "virtio0.rpmsg_ctrl.0.0";
    const char *RPMSG_BUS_SYS = "/sys/bus/rpmsg";

    rpmsg_handle_t* handle = (rpmsg_handle_t*)calloc(1, sizeof(rpmsg_handle_t));
    if (handle == NULL) {
        perror("Failed to allocate rpmsg_handle");
        return NULL;
    }

    // 绑定 rpmsg_chrdev 驱动 (简化版本，假设已加载)
    sprintf(fpath, "%s/devices/%s", RPMSG_BUS_SYS, rpmsg_dev);
    if (access(fpath, F_OK)) {
        fprintf(stderr, "RPMsg device not accessible: %s\n", fpath);
        free(handle);
        return NULL;
    }

    // 获取控制设备文件描述符
    DIR *dir;
    struct dirent *ent;
    sprintf(fpath, "%s/devices/%s/rpmsg", RPMSG_BUS_SYS, rpmsg_dev);
    dir = opendir(fpath);
    if (dir == NULL) {
        fprintf(stderr, "Failed to open rpmsg directory\n");
        free(handle);
        return NULL;
    }

    bool found = false;
    while ((ent = readdir(dir)) != NULL) {
        if (!strncmp(ent->d_name, "rpmsg_ctrl", strlen("rpmsg_ctrl"))) {
            sprintf(fpath, "/dev/%s", ent->d_name);
            handle->ctrl_fd = open(fpath, O_RDWR | O_NONBLOCK);
            if (handle->ctrl_fd < 0) {
                fprintf(stderr, "Failed to open rpmsg ctrl device\n");
                closedir(dir);
                free(handle);
                return NULL;
            }
            sprintf(rpmsg_char_name, "%s", ent->d_name);
            found = true;
            break;
        }
    }
    closedir(dir);

    if (!found) {
        fprintf(stderr, "rpmsg_ctrl device not found\n");
        free(handle);
        return NULL;
    }

    // 创建端点
    strcpy(eptinfo.name, "rpmsg-openamp-demo-channel");
    eptinfo.src = 0;
    eptinfo.dst = 0x400;
    if (rpmsg_create_ept(handle->ctrl_fd, &eptinfo) != 0) {
        fprintf(stderr, "Failed to create RPMsg endpoint\n");
        close(handle->ctrl_fd);
        free(handle);
        return NULL;
    }

    // 获取端点设备路径
    if (!get_rpmsg_ept_dev_name(rpmsg_char_name, eptinfo.name, ept_dev_name)) {
        fprintf(stderr, "Failed to get endpoint device name\n");
        close(handle->ctrl_fd);
        free(handle);
        return NULL;
    }

    sprintf(ept_dev_path, "/dev/%s", ept_dev_name);
    handle->ept_fd = open(ept_dev_path, O_RDWR | O_NONBLOCK);
    if (handle->ept_fd < 0) {
        perror("Failed to open rpmsg endpoint device");
        close(handle->ctrl_fd);
        free(handle);
        return NULL;
    }

    strncpy(handle->ept_path, ept_dev_path, sizeof(handle->ept_path) - 1);
    strncpy(handle->ept_name, eptinfo.name, sizeof(handle->ept_name) - 1);

    printf("RPMsg opened: ctrl=%d, ept=%d (%s)\n", handle->ctrl_fd, handle->ept_fd, ept_dev_path);
    return handle;
}

void rpmsg_close(rpmsg_handle_t* handle) {
    if (handle == NULL) return;

    if (handle->ept_fd >= 0) {
        close(handle->ept_fd);
    }
    if (handle->ctrl_fd >= 0) {
        close(handle->ctrl_fd);
    }
    free(handle);
}

int rpmsg_send(rpmsg_handle_t* handle, const void* data, size_t length) {
    if (handle == NULL || handle->ept_fd < 0) {
        return -1;
    }

    ssize_t sent = write(handle->ept_fd, data, length);
    if (sent < 0) {
        if (errno != EAGAIN && errno != EWOULDBLOCK) {
            perror("RPMsg send failed");
        }
        return -1;
    }
    return (int)sent;
}

int rpmsg_recv(rpmsg_handle_t* handle, void* data, size_t length, int timeout_ms) {
    if (handle == NULL || handle->ept_fd < 0) {
        return -1;
    }

    struct pollfd pfd;
    pfd.fd = handle->ept_fd;
    pfd.events = POLLIN;
    pfd.revents = 0;

    int ret = poll(&pfd, 1, timeout_ms);
    if (ret < 0) {
        perror("RPMsg poll failed");
        return -1;
    }
    if (ret == 0) {
        return 0;  // 超时
    }
    if (!(pfd.revents & POLLIN)) {
        return 0;
    }

    ssize_t received = read(handle->ept_fd, data, length);
    if (received < 0) {
        if (errno != EAGAIN && errno != EWOULDBLOCK) {
            perror("RPMsg recv failed");
        }
        return -1;
    }
    return (int)received;
}

int rpmsg_wait_for_cmd(rpmsg_handle_t* handle, uint32_t expected_cmd, int timeout_ms) {
    rpmsg_message_t msg;
    int elapsed = 0;
    const int poll_interval = 10;  // 10ms

    while (elapsed < timeout_ms || timeout_ms == 0) {
        int ret = rpmsg_recv(handle, &msg, sizeof(msg), poll_interval);
        if (ret < 0) {
            return -2;  // 错误
        }
        if (ret > 0 && msg.cmd == expected_cmd) {
            return 0;  // 成功
        }
        elapsed += poll_interval;
    }

    return -1;  // 超时
}
