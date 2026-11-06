/* Test-only sink for the documentation subnet 198.51.100.0/24. */
#include <errno.h>
#include <fcntl.h>
#include <linux/if_tun.h>
#include <net/if.h>
#include <signal.h>
#include <stdarg.h>
#include <stdio.h>
#include <string.h>
#include <sys/ioctl.h>
#include <syslog.h>
#include <unistd.h>

#include <dbus/dbus.h>

static volatile sig_atomic_t running = 1;

static void trace(const char *format, ...)
{
    va_list arguments;
    char message[1024];

    va_start(arguments, format);
    vsnprintf(message, sizeof(message), format, arguments);
    va_end(arguments);
    syslog(LOG_INFO, "pid=%ld %s", (long)getpid(), message);
}

static void stop(int signal_number)
{
    trace("signal=%d received; stopping read loop", signal_number);
    running = 0;
}

static int open_tunnel(const char *interface_name)
{
    struct ifreq request;
    int fd = open("/dev/net/tun", O_RDWR);
    trace("open /dev/net/tun fd=%d errno=%d", fd, fd < 0 ? errno : 0);
    if (fd < 0)
        return -1;
    memset(&request, 0, sizeof(request));
    request.ifr_flags = IFF_TUN | IFF_NO_PI;
    strncpy(request.ifr_name, interface_name, IFNAMSIZ - 1);
    if (ioctl(fd, TUNSETIFF, &request) < 0) {
        trace("ioctl TUNSETIFF interface=%s failed errno=%d", interface_name, errno);
        close(fd);
        return -1;
    }
    trace("ioctl TUNSETIFF interface=%s succeeded fd=%d", interface_name, fd);
    return fd;
}

static int notify_configuration(const char *bus_name, const char *interface_name,
                                const char *object_path)
{
    DBusConnection *connection;
    DBusError error;
    DBusMessage *message;
    DBusMessage *reply;
    DBusMessageIter arguments;
    DBusMessageIter variant;
    DBusMessageIter dictionary;
    DBusMessageIter entry;
    const char *reason = "configuration";
    const char *key = "Interface";
    dbus_bool_t sent;

    dbus_error_init(&error);
    connection = dbus_bus_get(DBUS_BUS_SYSTEM, &error);
    if (connection == NULL) {
        trace("D-Bus system connection failed name=%s message=%s",
              error.name != NULL ? error.name : "(none)",
              error.message != NULL ? error.message : "(none)");
        dbus_error_free(&error);
        return -1;
    }
    trace("D-Bus connected destination=%s interface=%s path=%s",
          bus_name, interface_name, object_path);
    /* Match the ConnMan task protocol expected by the Aurora SDK. */
    message = dbus_message_new_method_call(bus_name, object_path, interface_name, "notify");
    if (message == NULL) {
        trace("D-Bus Notify message allocation failed");
        dbus_connection_unref(connection);
        return -1;
    }

    dbus_message_iter_init_append(message, &arguments);
    dbus_message_iter_append_basic(&arguments, DBUS_TYPE_STRING, &reason);
    dbus_message_iter_open_container(&arguments, DBUS_TYPE_VARIANT, "a{ss}", &variant);
    dbus_message_iter_open_container(&variant, DBUS_TYPE_ARRAY, "{ss}", &dictionary);
    dbus_message_iter_open_container(&dictionary, DBUS_TYPE_DICT_ENTRY, NULL, &entry);
    dbus_message_iter_append_basic(&entry, DBUS_TYPE_STRING, &key);
    dbus_message_iter_append_basic(&entry, DBUS_TYPE_STRING, &interface_name);
    dbus_message_iter_close_container(&dictionary, &entry);
    dbus_message_iter_close_container(&variant, &dictionary);
    dbus_message_iter_close_container(&arguments, &variant);

    reply = dbus_connection_send_with_reply_and_block(connection, message, 5000, &error);
    sent = reply != NULL;
    if (reply != NULL)
        dbus_message_unref(reply);
    if (dbus_error_is_set(&error)) {
        trace("D-Bus notify failed name=%s message=%s", error.name, error.message);
        dbus_error_free(&error);
    }
    trace("D-Bus notify reason=%s interface=%s acknowledged=%d", reason, interface_name, sent);
    dbus_message_unref(message);
    dbus_connection_unref(connection);
    return sent ? 0 : -1;
}

int main(int argc, char **argv)
{
    const char *interface_name = NULL;
    const char *dbus_busname = NULL;
    const char *dbus_interface = NULL;
    const char *dbus_path = NULL;
    unsigned char buffer[2048];
    int fd;
    int index;

    openlog("aurora-test-route-provider", LOG_PID, LOG_DAEMON);
    trace("started argc=%d", argc);
    for (index = 0; index < argc; ++index)
        trace("argv[%d]=%s", index, argv[index]);
    for (index = 1; index + 1 < argc; ++index) {
        if (strcmp(argv[index], "--interface") == 0) {
            interface_name = argv[++index];
            trace("parsed interface=%s", interface_name);
        } else if (strcmp(argv[index], "--dbus-busname") == 0) {
            dbus_busname = argv[++index];
            trace("parsed dbus-busname=%s", dbus_busname);
        } else if (strcmp(argv[index], "--dbus-interface") == 0) {
            dbus_interface = argv[++index];
            trace("parsed dbus-interface=%s", dbus_interface);
        } else if (strcmp(argv[index], "--dbus-path") == 0) {
            dbus_path = argv[++index];
            trace("parsed dbus-path=%s", dbus_path);
        }
    }
    if (interface_name == NULL || dbus_busname == NULL || dbus_interface == NULL || dbus_path == NULL) {
        trace("startup rejected: required interface or D-Bus argument is missing");
        fprintf(stderr, "aurora-test-route-provider: --interface is required\n");
        return 2;
    }
    fd = open_tunnel(interface_name);
    if (fd < 0) {
        trace("attach failed interface=%s errno=%d", interface_name, errno);
        fprintf(stderr, "aurora-test-route-provider: cannot attach to %s: %s\n",
                interface_name, strerror(errno));
        return 3;
    }
    if (notify_configuration(dbus_busname, dbus_interface, dbus_path) != 0) {
        trace("D-Bus configuration notification failed");
        close(fd);
        return 4;
    }
    signal(SIGTERM, stop);
    signal(SIGINT, stop);
    trace("active interface=%s route=198.51.100.0/24 action=discard", interface_name);
    while (running) {
        ssize_t bytes = read(fd, buffer, sizeof(buffer));
        if (bytes < 0 && errno == EINTR) {
            trace("read interrupted; continue");
            continue;
        }
        if (bytes < 0) {
            trace("read failed errno=%d; leaving loop", errno);
            break;
        }
        trace("packet bytes=%ld firstByte=%u action=discard", (long)bytes,
              bytes > 0 ? (unsigned int)buffer[0] : 0);
    }
    trace("closing fd=%d", fd);
    close(fd);
    trace("exited normally");
    closelog();
    return 0;
}
