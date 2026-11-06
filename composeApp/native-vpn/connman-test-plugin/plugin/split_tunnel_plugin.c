/*
 * Deliberately constrained ConnMan VPN driver for emulator verification.
 * It creates a TUN-backed session and only advertises 198.51.100.0/24.
 * It does not set a default route or a DNS server and has no remote endpoint.
 */
#define CONNMAN_API_SUBJECT_TO_CHANGE

#include <errno.h>
#include <linux/if_tun.h>
#include <net/if.h>
#include <sys/socket.h>

#include <glib.h>

#include <connman/ipaddress.h>
#include <connman/dbus.h>
#include <connman/plugin.h>
#include <connman/task.h>
#include <connman/vpn-dbus.h>
#include <connman/vpn/plugins/vpn.h>
#include <connman/vpn/vpn-provider.h>

/*
 * The ConnMan shim identifies a driver by the descriptor name created by
 * CONNMAN_PLUGIN_DEFINE.  Keep this D-Bus type byte-for-byte identical to
 * that descriptor; hyphens here used to leave the shim without handler_data.
 */
#define DRIVER_NAME "aurora_test_route"
#define PROVIDER_PATH "/usr/bin/com.example.aurorakmpdemo.test-route-vpn"
#define TEST_NETWORK "198.51.100.0"
#define TEST_NETMASK "255.255.255.0"
#define TUN_ADDRESS "10.203.0.2"
#define TUN_PEER "10.203.0.1"
#define TUN_NETMASK "255.255.255.252"

struct test_session {
    struct vpn_provider *provider;
    struct connman_task *task;
};

static DBusConnection *connection;

/*
 * Aurora's VPN ABI completes a connection through this callback.  The
 * provider process emits net.connman.Task.Notify("configuration", ...),
 * and ConnMan advances the session after we acknowledge that notification.
 */
static int test_route_notify(DBusMessage *message, struct vpn_provider *provider)
{
    DBusMessageIter iterator;
    const char *reason = NULL;

    if (message == NULL || !dbus_message_iter_init(message, &iterator)) {
        connman_error("aurora-test-route: notify without a message");
        return VPN_STATE_FAILURE;
    }
    if (dbus_message_iter_get_arg_type(&iterator) != DBUS_TYPE_STRING) {
        connman_error("aurora-test-route: notify has invalid first argument type=%c",
                      dbus_message_iter_get_arg_type(&iterator));
        return VPN_STATE_FAILURE;
    }
    dbus_message_iter_get_basic(&iterator, &reason);
    connman_info("aurora-test-route: notify reason=%s provider=%p",
                 reason != NULL ? reason : "(null)", provider);

    if (g_strcmp0(reason, "configuration") == 0) {
        connman_info("aurora-test-route: configuration notification acknowledged; state=CONNECT");
        return VPN_STATE_CONNECT;
    }

    connman_error("aurora-test-route: unsupported notify reason=%s",
                  reason != NULL ? reason : "(null)");
    return VPN_STATE_FAILURE;
}

static void test_route_died(struct connman_task *task, int exit_code, void *user_data)
{
    struct test_session *session = user_data;
    connman_info("aurora-test-route: provider exit callback task=%p code=%d session=%p",
                 task, exit_code, session);
    if (session == NULL)
        return;

    connman_info("aurora-test-route: forwarding exit to ConnMan state machine");
    vpn_died(task, exit_code, session->provider);
    connman_info("aurora-test-route: releasing retained provider reference");
    vpn_provider_unref(session->provider);
    g_free(session);
}

static int test_route_connect(struct vpn_provider *provider,
                              struct connman_task *task,
                              const char *if_name,
                              vpn_provider_connect_cb_t cb,
                              const char *dbus_sender,
                              void *user_data)
{
    struct connman_ipaddress *ipaddress;
    struct test_session *session;
    int error;

    connman_info("aurora-test-route: connect provider=%p task=%p interface=%s sender=%s userData=%p",
                 provider, task, if_name, dbus_sender, user_data);
    session = g_try_new0(struct test_session, 1);
    if (session == NULL) {
        connman_error("aurora-test-route: session allocation failed");
        return -ENOMEM;
    }

    session->provider = vpn_provider_ref(provider);
    session->task = task;
    vpn_provider_set_plugin_data(provider, session);
    connman_info("aurora-test-route: session=%p retained and bound to provider", session);

    if (connection == NULL) {
        connman_error("aurora-test-route: system D-Bus connection is unavailable");
        vpn_provider_unref(session->provider);
        g_free(session);
        return -EIO;
    }

    /* Let the provider report readiness through ConnMan's required task ABI. */
    error = connman_task_add_argument(task, "--dbus-busname",
                                      dbus_bus_get_unique_name(connection));
    connman_info("aurora-test-route: argument dbus-busname result=%d", error);
    error = connman_task_add_argument(task, "--dbus-interface", CONNMAN_TASK_INTERFACE);
    connman_info("aurora-test-route: argument dbus-interface=%s result=%d",
                 CONNMAN_TASK_INTERFACE, error);
    error = connman_task_add_argument(task, "--dbus-path", connman_task_get_path(task));
    connman_info("aurora-test-route: argument dbus-path=%s result=%d",
                 connman_task_get_path(task), error);

    /* ConnMan creates the TUN device; the provider keeps it open and drains it. */
    error = connman_task_add_argument(task, "--interface", if_name);
    connman_info("aurora-test-route: argument interface=%s result=%d", if_name, error);
    error = connman_task_add_argument(task, "--network", TEST_NETWORK "/24");
    connman_info("aurora-test-route: argument network=%s/24 result=%d", TEST_NETWORK, error);

    ipaddress = connman_ipaddress_alloc(AF_INET);
    if (ipaddress == NULL) {
        connman_error("aurora-test-route: IPv4 configuration allocation failed");
        vpn_provider_unref(session->provider);
        g_free(session);
        return -ENOMEM;
    }

    error = connman_ipaddress_set_ipv4(ipaddress, TUN_ADDRESS, TUN_NETMASK, NULL);
    connman_info("aurora-test-route: address=%s mask=%s result=%d", TUN_ADDRESS, TUN_NETMASK, error);
    connman_ipaddress_set_peer(ipaddress, TUN_PEER);
    connman_info("aurora-test-route: peer=%s configured", TUN_PEER);
    error = vpn_provider_set_ipaddress(provider, ipaddress);
    connman_info("aurora-test-route: handed IPv4 configuration to ConnMan result=%d", error);
    connman_ipaddress_free(ipaddress);

    /* No default route and no nameserver are ever supplied by this driver. */
    error = vpn_provider_set_boolean(provider, "SplitRouting", true, false);
    connman_info("aurora-test-route: SplitRouting=true (boolean) result=%d", error);
    error = vpn_provider_append_route(provider, "route_network_1", TEST_NETWORK);
    connman_info("aurora-test-route: route network=%s result=%d", TEST_NETWORK, error);
    error = vpn_provider_append_route(provider, "route_netmask_1", TEST_NETMASK);
    connman_info("aurora-test-route: route netmask=%s result=%d", TEST_NETMASK, error);
    error = vpn_provider_append_route(provider, "route_gateway_1", TUN_PEER);
    connman_info("aurora-test-route: route gateway=%s result=%d", TUN_PEER, error);
    connman_info("aurora-test-route: deliberately no DNS and no default route supplied");

    error = connman_task_run(task, test_route_died, session, NULL, NULL, NULL);
    if (error < 0) {
        connman_error("aurora-test-route: provider process launch failed result=%d", error);
        vpn_provider_unref(session->provider);
        g_free(session);
        return error;
    }

    connman_info("aurora-test-route: provider process launched result=%d", error);
    if (cb != NULL) {
        connman_info("aurora-test-route: invoking connect callback");
        cb(provider, user_data, 0);
    } else {
        connman_error("aurora-test-route: connect callback missing");
    }

    connman_info("aurora-test-route: waiting for provider configuration notification interface=%s route=%s/24",
                 if_name, TEST_NETWORK);
    return 0;
}

static void test_route_disconnect(struct vpn_provider *provider)
{
    struct test_session *session = vpn_provider_get_plugin_data(provider);
    int error;
    connman_info("aurora-test-route: disconnect provider=%p session=%p", provider, session);
    error = vpn_provider_set_state(provider, VPN_PROVIDER_STATE_DISCONNECT);
    connman_info("aurora-test-route: state=DISCONNECT result=%d", error);
    if (session != NULL && session->task != NULL) {
        error = connman_task_stop(session->task);
        connman_info("aurora-test-route: provider process stop result=%d", error);
    } else {
        connman_info("aurora-test-route: no provider process to stop");
    }
}

static int test_route_error_code(struct vpn_provider *provider, int exit_code)
{
    connman_info("aurora-test-route: map process exit provider=%p code=%d", provider, exit_code);
    return exit_code == 0 ? VPN_PROVIDER_ERROR_UNKNOWN : VPN_PROVIDER_ERROR_CONNECT_FAILED;
}

/*
 * ConnMan's Aurora shim asks every TUN-based provider for its interface
 * flags before it builds the per-connection task.  Leaving this callback
 * empty makes the shim create no handler context; a subsequent Connect then
 * aborts connman-vpnd instead of returning a normal connection error.
 *
 * This follows the ConnMan VPN-provider contract used by the Aurora SDK sample.
 */
static int test_route_device_flags(struct vpn_provider *provider)
{
    connman_info("aurora-test-route: device flags requested provider=%p flags=IFF_TUN",
                 provider);
    return IFF_TUN;
}

static const struct vpn_driver test_route_driver = {
    .notify = test_route_notify,
    .connect = test_route_connect,
    .disconnect = test_route_disconnect,
    .error_code = test_route_error_code,
    .device_flags = test_route_device_flags,
};

static int test_route_init(void)
{
    connection = connman_dbus_get_connection();
    if (connection == NULL) {
        connman_error("aurora-test-route: unable to acquire ConnMan system D-Bus connection");
        return -EIO;
    }
    connman_info("aurora-test-route: register route=%s/24 defaultRoute=unchanged dns=unchanged provider=%s",
                 TEST_NETWORK, PROVIDER_PATH);
    return vpn_register(DRIVER_NAME, &test_route_driver, PROVIDER_PATH);
}

static void test_route_exit(void)
{
    connman_info("aurora-test-route: unregistering split-tunnel driver");
    vpn_unregister(DRIVER_NAME);
}

CONNMAN_PLUGIN_DEFINE(aurora_test_route, "Aurora split-tunnel test provider",
                      CONNMAN_VERSION, CONNMAN_PLUGIN_PRIORITY_DEFAULT,
                      test_route_init, test_route_exit)
