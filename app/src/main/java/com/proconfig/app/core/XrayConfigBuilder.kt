package com.proconfig.app.core

import org.json.JSONArray
import org.json.JSONObject

object XrayConfigBuilder {

    fun buildFullConfig(c: ProxyConfig, socksPort: Int, httpPort: Int): String {
        val root = JSONObject()

        val log = JSONObject()
        log.put("loglevel", "warning")
        root.put("log", log)

        val inbounds = JSONArray()

        val socks = JSONObject()
        socks.put("tag", "socks-in")
        socks.put("listen", "127.0.0.1")
        socks.put("port", socksPort)
        socks.put("protocol", "socks")
        val socksSettings = JSONObject()
        socksSettings.put("udp", true)
        socksSettings.put("auth", "noauth")
        socks.put("settings", socksSettings)
        socks.put("sniffing", JSONObject().put("enabled", true).put("destOverride", JSONArray().put("http").put("tls")))
        inbounds.put(socks)

        val http = JSONObject()
        http.put("tag", "http-in")
        http.put("listen", "127.0.0.1")
        http.put("port", httpPort)
        http.put("protocol", "http")
        http.put("settings", JSONObject().put("allowTransparent", false))
        inbounds.put(http)

        root.put("inbounds", inbounds)

        val outbounds = JSONArray()
        outbounds.put(buildOutbound(c))

        val direct = JSONObject()
        direct.put("tag", "direct")
        direct.put("protocol", "freedom")
        direct.put("settings", JSONObject())
        outbounds.put(direct)

        val block = JSONObject()
        block.put("tag", "block")
        block.put("protocol", "blackhole")
        block.put("settings", JSONObject())
        outbounds.put(block)

        root.put("outbounds", outbounds)

        val routing = JSONObject()
        routing.put("domainStrategy", "AsIs")
        val rules = JSONArray()

        val privateRule = JSONObject()
        privateRule.put("type", "field")
        privateRule.put(
            "ip",
            JSONArray()
                .put("0.0.0.0/8")
                .put("10.0.0.0/8")
                .put("100.64.0.0/10")
                .put("127.0.0.0/8")
                .put("169.254.0.0/16")
                .put("172.16.0.0/12")
                .put("192.0.0.0/24")
                .put("192.168.0.0/16")
                .put("198.18.0.0/15")
                .put("224.0.0.0/3")
                .put("::1/128")
                .put("fc00::/7")
                .put("fe80::/10")
        )
        privateRule.put("outboundTag", "direct")
        rules.put(privateRule)

        val blockRule = JSONObject()
        blockRule.put("type", "field")
        blockRule.put("protocol", JSONArray().put("bittorrent"))
        blockRule.put("outboundTag", "block")
        rules.put(blockRule)

        routing.put("rules", rules)
        root.put("routing", routing)

        return root.toString()
    }

    fun buildOutbound(c: ProxyConfig): JSONObject {
        val outbound = JSONObject()
        outbound.put("tag", "proxy")

        val stream = JSONObject()
        stream.put("network", mapNetwork(c.network))

        when (c.proto) {
            ProxyConfig.PROTO_VMESS -> {
                outbound.put("protocol", "vmess")
                val settings = JSONObject()
                val vnext = JSONArray()
                val server = JSONObject()
                server.put("address", c.address)
                server.put("port", c.port)
                val users = JSONArray()
                val user = JSONObject()
                user.put("id", c.uuid)
                user.put("security", "auto")
                user.put("alterId", 0)
                user.put("level", 0)
                users.put(user)
                server.put("users", users)
                vnext.put(server)
                settings.put("vnext", vnext)
                outbound.put("settings", settings)
            }
            ProxyConfig.PROTO_VLESS -> {
                outbound.put("protocol", "vless")
                val settings = JSONObject()
                val vnext = JSONArray()
                val server = JSONObject()
                server.put("address", c.address)
                server.put("port", c.port)
                val users = JSONArray()
                val user = JSONObject()
                user.put("id", c.uuid)
                user.put("encryption", "none")
                if (c.flow.isNotEmpty()) user.put("flow", c.flow)
                user.put("level", 0)
                users.put(user)
                server.put("users", users)
                vnext.put(server)
                settings.put("vnext", vnext)
                outbound.put("settings", settings)
            }
            ProxyConfig.PROTO_TROJAN -> {
                outbound.put("protocol", "trojan")
                val settings = JSONObject()
                val servers = JSONArray()
                val server = JSONObject()
                server.put("address", c.address)
                server.put("port", c.port)
                server.put("password", c.password)
                server.put("level", 0)
                servers.put(server)
                settings.put("servers", servers)
                outbound.put("settings", settings)
            }
            ProxyConfig.PROTO_SS -> {
                outbound.put("protocol", "shadowsocks")
                val settings = JSONObject()
                val servers = JSONArray()
                val server = JSONObject()
                server.put("address", c.address)
                server.put("port", c.port)
                val method = c.password.substringBefore(':')
                val password = c.password.substringAfter(':')
                server.put("method", method)
                server.put("password", password)
                server.put("level", 0)
                servers.put(server)
                settings.put("servers", servers)
                outbound.put("settings", settings)
            }
        }

        when (c.security) {
            ProxyConfig.SEC_TLS -> {
                val tlsSettings = JSONObject()
                if (c.sni.isNotEmpty()) tlsSettings.put("serverName", c.sni)
                if (c.fingerprint.isNotEmpty()) tlsSettings.put("fingerprint", c.fingerprint)
                tlsSettings.put("allowInsecure", c.allowInsecure)
                stream.put("security", "tls")
                stream.put("tlsSettings", tlsSettings)
            }
            ProxyConfig.SEC_REALITY -> {
                val realitySettings = JSONObject()
                if (c.sni.isNotEmpty()) realitySettings.put("serverName", c.sni)
                if (c.fingerprint.isEmpty()) {
                    realitySettings.put("fingerprint", "chrome")
                } else {
                    realitySettings.put("fingerprint", c.fingerprint)
                }
                realitySettings.put("publicKey", c.publicKey)
                realitySettings.put("shortId", c.shortId)
                if (c.spiderX.isNotEmpty()) realitySettings.put("spiderX", c.spiderX)
                stream.put("security", "reality")
                stream.put("realitySettings", realitySettings)
            }
            else -> stream.put("security", "none")
        }

        when (c.network) {
            ProxyConfig.NET_WS -> {
                val ws = JSONObject()
                if (c.path.isNotEmpty()) ws.put("path", c.path)
                if (c.host.isNotEmpty()) {
                    val headers = JSONObject()
                    headers.put("Host", c.host)
                    ws.put("headers", headers)
                }
                stream.put("wsSettings", ws)
            }
            ProxyConfig.NET_GRPC -> {
                val grpc = JSONObject()
                if (c.serviceName.isNotEmpty()) grpc.put("serviceName", c.serviceName)
                grpc.put("multiMode", false)
                grpc.put("idle_timeout", 60)
                stream.put("grpcSettings", grpc)
            }
            ProxyConfig.NET_H2 -> {
                val h2 = JSONObject()
                if (c.path.isNotEmpty()) h2.put("path", c.path)
                if (c.host.isNotEmpty()) {
                    val hosts = JSONArray()
                    hosts.put(c.host)
                    h2.put("host", hosts)
                }
                stream.put("httpSettings", h2)
            }
            else -> {
                stream.put("network", "tcp")
            }
        }

        outbound.put("streamSettings", stream)
        return outbound
    }

    private fun mapNetwork(n: String): String = when (n) {
        ProxyConfig.NET_H2 -> "http"
        else -> n
    }
}
