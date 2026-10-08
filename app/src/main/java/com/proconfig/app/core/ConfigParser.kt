package com.proconfig.app.core

import android.util.Base64
import java.net.URLDecoder

object ConfigParser {

    fun parse(link: String): ProxyConfig? {
        val l = link.trim()
        if (l.isEmpty()) return null
        return when {
            l.startsWith("vmess://") -> parseVmess(l)
            l.startsWith("vless://") -> parseVless(l)
            l.startsWith("trojan://") -> parseTrojan(l)
            l.startsWith("ss://") -> parseShadowsocks(l)
            else -> null
        }
    }

    private fun b64decode(data: String): String? {
        return try {
            val cleaned = data.replace("-", "+").replace("_", "/").trim()
            val padded = when (cleaned.length % 4) {
                2 -> "$cleaned=="
                3 -> "$cleaned="
                else -> cleaned
            }
            String(Base64.decode(padded, Base64.DEFAULT))
        } catch (e: Exception) {
            null
        }
    }

    private fun queryParams(uriPart: String): Map<String, String> {
        val map = HashMap<String, String>()
        val qIndex = uriPart.indexOf('?')
        if (qIndex < 0) return map
        val query = uriPart.substring(qIndex + 1).substringBefore('#')
        for (pair in query.split('&')) {
            if (pair.isEmpty()) continue
            val idx = pair.indexOf('=')
            if (idx > 0) {
                val k = URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                val v = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                map[k] = v
            }
        }
        return map
    }

    private fun fragmentName(uriPart: String): String {
        val h = uriPart.indexOf('#')
        if (h < 0) return ""
        return try {
            URLDecoder.decode(uriPart.substring(h + 1), "UTF-8")
        } catch (e: Exception) {
            uriPart.substring(h + 1)
        }
    }

    private fun parseVmess(link: String): ProxyConfig? {
        try {
            val body = link.removePrefix("vmess://")
            val json = b64decode(body) ?: return null
            val o = org.json.JSONObject(json)
            val c = ProxyConfig()
            c.id = ProxyConfig.newId()
            c.proto = ProxyConfig.PROTO_VMESS
            c.name = o.optString("ps", "VMess")
            c.address = o.optString("add", "")
            c.port = o.optInt("port", 443)
            c.uuid = o.optString("id", "")
            c.network = o.optString("net", ProxyConfig.NET_TCP).ifEmpty { ProxyConfig.NET_TCP }
            c.host = o.optString("host", "")
            c.path = o.optString("path", "")
            c.serviceName = o.optString("path", "")
            val tls = o.optString("tls", "")
            c.security = if (tls == "tls" || tls == "reality") tls else ProxyConfig.SEC_NONE
            c.sni = o.optString("sni", "")
            if (c.sni.isEmpty()) c.sni = o.optString("host", "")
            c.fingerprint = o.optString("fp", "")
            c.allowInsecure = o.optBoolean("allowInsecure", false)
            if (c.address.isEmpty() || c.uuid.isEmpty()) return null
            return c
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseVless(link: String): ProxyConfig? {
        try {
            val body = link.removePrefix("vless://")
            val hashIdx = body.indexOf('#')
            val mainPart = if (hashIdx >= 0) body.substring(0, hashIdx) else body
            val name = fragmentName(body)
            val atIdx = mainPart.lastIndexOf('@')
            if (atIdx < 0) return null
            val uuid = mainPart.substring(0, atIdx)
            val hostPort = mainPart.substring(atIdx + 1).substringBefore('?')
            val host = hostPort.substringBeforeLast(':')
            val port = hostPort.substringAfterLast(':').toIntOrNull() ?: return null
            val q = queryParams(mainPart)

            val c = ProxyConfig()
            c.id = ProxyConfig.newId()
            c.proto = ProxyConfig.PROTO_VLESS
            c.name = if (name.isEmpty()) "VLESS" else name
            c.uuid = uuid
            c.address = host
            c.port = port
            c.security = q["security"] ?: ProxyConfig.SEC_TLS
            if (c.security == "none") c.security = ProxyConfig.SEC_NONE
            c.network = q["type"] ?: ProxyConfig.NET_TCP
            c.sni = q["sni"] ?: ""
            if (c.sni.isEmpty()) c.sni = q["host"] ?: ""
            c.host = q["host"] ?: ""
            c.path = q["path"] ?: ""
            c.serviceName = q["serviceName"] ?: ""
            c.flow = q["flow"] ?: ""
            c.fingerprint = q["fp"] ?: ""
            c.publicKey = q["pbk"] ?: ""
            c.shortId = q["sid"] ?: ""
            c.spiderX = q["spx"] ?: ""
            c.allowInsecure = q["allowInsecure"] == "1" || q["allowInsecure"] == "true"
            if (c.address.isEmpty()) return null
            return c
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseTrojan(link: String): ProxyConfig? {
        try {
            val body = link.removePrefix("trojan://")
            val hashIdx = body.indexOf('#')
            val mainPart = if (hashIdx >= 0) body.substring(0, hashIdx) else body
            val name = fragmentName(body)
            val atIdx = mainPart.lastIndexOf('@')
            if (atIdx < 0) return null
            val password = URLDecoder.decode(mainPart.substring(0, atIdx), "UTF-8")
            val hostPort = mainPart.substring(atIdx + 1).substringBefore('?')
            val host = hostPort.substringBeforeLast(':')
            val port = hostPort.substringAfterLast(':').toIntOrNull() ?: return null
            val q = queryParams(mainPart)

            val c = ProxyConfig()
            c.id = ProxyConfig.newId()
            c.proto = ProxyConfig.PROTO_TROJAN
            c.name = if (name.isEmpty()) "Trojan" else name
            c.password = password
            c.address = host
            c.port = port
            c.security = if (q["security"] == "none") ProxyConfig.SEC_NONE else ProxyConfig.SEC_TLS
            c.network = q["type"] ?: ProxyConfig.NET_TCP
            c.sni = q["sni"] ?: ""
            if (c.sni.isEmpty()) c.sni = q["peer"] ?: ""
            c.host = q["host"] ?: ""
            c.path = q["path"] ?: ""
            c.serviceName = q["serviceName"] ?: ""
            c.fingerprint = q["fp"] ?: ""
            c.allowInsecure = q["allowInsecure"] == "1" || q["allowInsecure"] == "true"
            if (c.address.isEmpty()) return null
            return c
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseShadowsocks(link: String): ProxyConfig? {
        try {
            var body = link.removePrefix("ss://")
            val name: String
            val hashIdx = body.indexOf('#')
            if (hashIdx >= 0) {
                name = fragmentName(body)
                body = body.substring(0, hashIdx)
            } else {
                name = "Shadowsocks"
            }
            val q = queryParams(body)
            body = body.substringBefore('?')
            // plugin=... is in query; ignore plugins

            val atIdx = body.lastIndexOf('@')
            val method: String
            val password: String
            val hostPort: String
            if (atIdx >= 0) {
                val userInfo = body.substring(0, atIdx)
                hostPort = body.substring(atIdx + 1)
                val decoded = b64decode(userInfo) ?: URLDecoder.decode(userInfo, "UTF-8")
                method = decoded.substringBefore(':')
                password = decoded.substringAfter(':')
            } else {
                val decoded = b64decode(body) ?: return null
                val lastAt = decoded.lastIndexOf('@')
                if (lastAt < 0) return null
                method = decoded.substring(0, lastAt).substringBefore(':')
                password = decoded.substring(0, lastAt).substringAfter(':')
                hostPort = decoded.substring(lastAt + 1)
            }
            val host = hostPort.substringBeforeLast(':')
            val port = hostPort.substringAfterLast(':').trim().trimEnd(']').toIntOrNull() ?: return null

            val c = ProxyConfig()
            c.id = ProxyConfig.newId()
            c.proto = ProxyConfig.PROTO_SS
            c.name = name
            c.password = "$method:$password"
            c.address = host
            c.port = port
            c.security = ProxyConfig.SEC_NONE
            c.network = ProxyConfig.NET_TCP
            if (c.address.isEmpty()) return null
            return c
        } catch (e: Exception) {
            return null
        }
    }

    fun exportLink(c: ProxyConfig): String {
        return when (c.proto) {
            ProxyConfig.PROTO_VMESS -> exportVmess(c)
            ProxyConfig.PROTO_VLESS -> exportVless(c)
            ProxyConfig.PROTO_TROJAN -> exportTrojan(c)
            ProxyConfig.PROTO_SS -> exportSs(c)
            else -> ""
        }
    }

    private fun b64encode(data: String): String {
        return Base64.encodeToString(data.toByteArray(), Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE)
    }

    private fun exportVmess(c: ProxyConfig): String {
        val o = org.json.JSONObject()
        o.put("v", "2")
        o.put("ps", c.name)
        o.put("add", c.address)
        o.put("port", c.port)
        o.put("id", c.uuid)
        o.put("aid", 0)
        o.put("scy", "auto")
        o.put("net", c.network)
        o.put("type", "none")
        o.put("host", c.host)
        o.put("path", if (c.network == ProxyConfig.NET_GRPC) c.serviceName else c.path)
        o.put("tls", c.security)
        o.put("sni", c.sni)
        o.put("fp", c.fingerprint)
        if (c.allowInsecure) o.put("allowInsecure", true)
        return "vmess://" + b64encode(o.toString())
    }

    private fun exportVless(c: ProxyConfig): String {
        val sb = StringBuilder()
        sb.append("vless://").append(c.uuid).append('@').append(c.address).append(':').append(c.port)
        val params = ArrayList<String>()
        params.add("encryption=none")
        params.add("security=" + c.security)
        if (c.sni.isNotEmpty()) params.add("sni=" + c.sni)
        if (c.fingerprint.isNotEmpty()) params.add("fp=" + c.fingerprint)
        if (c.publicKey.isNotEmpty()) params.add("pbk=" + c.publicKey)
        if (c.shortId.isNotEmpty()) params.add("sid=" + c.shortId)
        if (c.spiderX.isNotEmpty()) params.add("spx=" + c.spiderX)
        params.add("type=" + c.network)
        if (c.host.isNotEmpty()) params.add("host=" + c.host)
        if (c.network == ProxyConfig.NET_GRPC) {
            if (c.serviceName.isNotEmpty()) params.add("serviceName=" + c.serviceName)
        } else if (c.path.isNotEmpty()) {
            params.add("path=" + c.path)
        }
        if (c.flow.isNotEmpty()) params.add("flow=" + c.flow)
        if (c.allowInsecure) params.add("allowInsecure=1")
        sb.append('?').append(params.joinToString("&"))
        sb.append('#').append(java.net.URLEncoder.encode(c.name, "UTF-8"))
        return sb.toString()
    }

    private fun exportTrojan(c: ProxyConfig): String {
        val sb = StringBuilder()
        sb.append("trojan://").append(java.net.URLEncoder.encode(c.password, "UTF-8"))
            .append('@').append(c.address).append(':').append(c.port)
        val params = ArrayList<String>()
        if (c.security != ProxyConfig.SEC_NONE) params.add("security=tls")
        if (c.sni.isNotEmpty()) params.add("sni=" + c.sni)
        if (c.fingerprint.isNotEmpty()) params.add("fp=" + c.fingerprint)
        params.add("type=" + c.network)
        if (c.host.isNotEmpty()) params.add("host=" + c.host)
        if (c.network == ProxyConfig.NET_GRPC) {
            if (c.serviceName.isNotEmpty()) params.add("serviceName=" + c.serviceName)
        } else if (c.path.isNotEmpty()) {
            params.add("path=" + c.path)
        }
        if (c.allowInsecure) params.add("allowInsecure=1")
        sb.append('?').append(params.joinToString("&"))
        sb.append('#').append(java.net.URLEncoder.encode(c.name, "UTF-8"))
        return sb.toString()
    }

    private fun exportSs(c: ProxyConfig): String {
        val method = c.password.substringBefore(':')
        val password = c.password.substringAfter(':')
        val userInfo = b64encode("$method:$password")
        return "ss://" + userInfo + "@" + c.address + ":" + c.port +
                "#" + java.net.URLEncoder.encode(c.name, "UTF-8")
    }
}
