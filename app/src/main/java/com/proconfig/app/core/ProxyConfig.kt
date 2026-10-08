package com.proconfig.app.core

data class ProxyConfig(
    var id: String = "",
    var name: String = "",
    var proto: String = PROTO_VLESS,
    var address: String = "",
    var port: Int = 443,
    var uuid: String = "",
    var password: String = "",
    var security: String = SEC_TLS,
    var network: String = NET_TCP,
    var sni: String = "",
    var host: String = "",
    var path: String = "",
    var serviceName: String = "",
    var flow: String = "",
    var fingerprint: String = "",
    var publicKey: String = "",
    var shortId: String = "",
    var spiderX: String = "",
    var allowInsecure: Boolean = false,
    var latency: Int = -1,
    var subId: String = ""
) {
    fun displayAddress(): String = "$address:$port"

    fun protoLabel(): String = when (proto) {
        PROTO_VMESS -> "VMess"
        PROTO_VLESS -> "VLESS"
        PROTO_TROJAN -> "Trojan"
        PROTO_SS -> "Shadowsocks"
        else -> proto.uppercase()
    }

    companion object {
        const val PROTO_VMESS = "vmess"
        const val PROTO_VLESS = "vless"
        const val PROTO_TROJAN = "trojan"
        const val PROTO_SS = "shadowsocks"

        const val SEC_TLS = "tls"
        const val SEC_REALITY = "reality"
        const val SEC_NONE = "none"

        const val NET_TCP = "tcp"
        const val NET_WS = "ws"
        const val NET_GRPC = "grpc"
        const val NET_H2 = "h2"

        fun newId(): String =
            java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16)
    }
}

data class Subscription(
    var id: String = "",
    var name: String = "",
    var url: String = "",
    var lastUpdate: Long = 0,
    var count: Int = 0
)
