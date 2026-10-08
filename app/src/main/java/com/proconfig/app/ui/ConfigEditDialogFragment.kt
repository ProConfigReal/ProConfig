package com.proconfig.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.proconfig.app.R
import com.proconfig.app.core.ConfigRepository
import com.proconfig.app.core.ProxyConfig
import com.proconfig.app.databinding.DialogConfigEditBinding

class ConfigEditDialogFragment(
    private val existing: ProxyConfig?,
    private val onSaved: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogConfigEditBinding? = null
    private val b get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogConfigEditBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val protos = resources.getStringArray(R.array.proto_entries)
        val protoValues = resources.getStringArray(R.array.proto_values)
        val securities = resources.getStringArray(R.array.security_entries)
        val securityValues = resources.getStringArray(R.array.security_values)
        val networks = resources.getStringArray(R.array.network_entries)
        val networkValues = resources.getStringArray(R.array.network_values)

        b.spProto.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, protos)
        )
        b.spSecurity.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, securities)
        )
        b.spNetwork.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, networks)
        )

        if (existing != null) {
            b.etName.setText(existing.name)
            b.etAddress.setText(existing.address)
            b.etPort.setText(existing.port.toString())
            b.etUuid.setText(if (existing.proto == ProxyConfig.PROTO_TROJAN || existing.proto == ProxyConfig.PROTO_SS) existing.password else existing.uuid)
            val protoIdx = protoValues.indexOf(existing.proto)
            b.spProto.setText(if (protoIdx >= 0) protos[protoIdx] else protos[0], false)
            val secIdx = securityValues.indexOf(existing.security)
            b.spSecurity.setText(if (secIdx >= 0) securities[secIdx] else securities[0], false)
            val netIdx = networkValues.indexOf(existing.network)
            b.spNetwork.setText(if (netIdx >= 0) networks[netIdx] else networks[0], false)
            b.etSni.setText(existing.sni)
            b.etHost.setText(existing.host)
            b.etPath.setText(if (existing.network == ProxyConfig.NET_GRPC) existing.serviceName else existing.path)
            b.etFlow.setText(existing.flow)
            b.etFingerprint.setText(existing.fingerprint)
            b.etPublicKey.setText(existing.publicKey)
            b.etShortId.setText(existing.shortId)
            b.cbInsecure.isChecked = existing.allowInsecure
        } else {
            b.spProto.setText(protos[1], false)
            b.spSecurity.setText(securities[0], false)
            b.spNetwork.setText(networks[0], false)
        }

        b.spProto.setOnItemClickListener { _, _, position, _ ->
            when (protoValues.getOrNull(position)) {
                ProxyConfig.PROTO_TROJAN, ProxyConfig.PROTO_SS -> {
                    b.layoutSecurity.visibility = View.GONE
                }
                ProxyConfig.PROTO_VMESS -> {
                    b.layoutSecurity.visibility = View.VISIBLE
                    b.layoutFlow.visibility = View.GONE
                }
                else -> {
                    b.layoutSecurity.visibility = View.VISIBLE
                    b.layoutFlow.visibility = View.VISIBLE
                }
            }
            if (protoValues.getOrNull(position) == ProxyConfig.PROTO_SS) {
                b.layoutSecurity.visibility = View.GONE
                b.layoutFlow.visibility = View.GONE
            }
        }

        b.btnCancel.setOnClickListener { dismiss() }
        b.btnSave.setOnClickListener { save() }
    }

    private fun save() {
        val name = b.etName.text?.toString()?.trim() ?: ""
        val address = b.etAddress.text?.toString()?.trim() ?: ""
        val port = b.etPort.text?.toString()?.trim()?.toIntOrNull() ?: 0
        val secret = b.etUuid.text?.toString()?.trim() ?: ""

        if (name.isEmpty() || address.isEmpty() || port <= 0 || secret.isEmpty()) {
            b.tvError.visibility = View.VISIBLE
            return
        }
        b.tvError.visibility = View.GONE

        val protoValues = resources.getStringArray(R.array.proto_values)
        val securityValues = resources.getStringArray(R.array.security_values)
        val networkValues = resources.getStringArray(R.array.network_values)
        val protos = resources.getStringArray(R.array.proto_entries)
        val securities = resources.getStringArray(R.array.security_entries)
        val networks = resources.getStringArray(R.array.network_entries)

        val proto = protoValues.getOrNull(protos.indexOf(b.spProto.text.toString())) ?: ProxyConfig.PROTO_VLESS
        val security = securityValues.getOrNull(securities.indexOf(b.spSecurity.text.toString())) ?: ProxyConfig.SEC_TLS
        val network = networkValues.getOrNull(networks.indexOf(b.spNetwork.text.toString())) ?: ProxyConfig.NET_TCP

        val c = existing ?: ProxyConfig(id = ProxyConfig.newId())
        c.name = name
        c.proto = proto
        c.address = address
        c.port = port
        if (proto == ProxyConfig.PROTO_TROJAN || proto == ProxyConfig.PROTO_SS) {
            c.password = secret
            c.uuid = ""
        } else {
            c.uuid = secret
            c.password = ""
        }
        c.security = if (proto == ProxyConfig.PROTO_TROJAN || proto == ProxyConfig.PROTO_SS) ProxyConfig.SEC_TLS else security
        c.network = network
        c.sni = b.etSni.text?.toString()?.trim() ?: ""
        c.host = b.etHost.text?.toString()?.trim() ?: ""
        if (network == ProxyConfig.NET_GRPC) {
            c.serviceName = b.etPath.text?.toString()?.trim() ?: ""
            c.path = ""
        } else {
            c.path = b.etPath.text?.toString()?.trim() ?: ""
            c.serviceName = ""
        }
        c.flow = b.etFlow.text?.toString()?.trim() ?: ""
        c.fingerprint = b.etFingerprint.text?.toString()?.trim() ?: ""
        c.publicKey = b.etPublicKey.text?.toString()?.trim() ?: ""
        c.shortId = b.etShortId.text?.toString()?.trim() ?: ""
        c.allowInsecure = b.cbInsecure.isChecked

        if (existing == null) {
            context?.let { ConfigRepository.addConfig(it, c) }
        } else {
            context?.let { ConfigRepository.updateConfig(it, c) }
        }
        onSaved()
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
