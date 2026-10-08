package com.proconfig.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.proconfig.app.App
import com.proconfig.app.R
import com.proconfig.app.databinding.FragmentSettingsBinding
import com.proconfig.app.util.Prefs

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val b get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        b.etSocksPort.setText(Prefs.socksPort.toString())
        b.etMtu.setText(Prefs.mtu.toString())
        b.switchAutoConnect.isChecked = Prefs.autoConnect

        b.btnApplyCore.setOnClickListener {
            val port = b.etSocksPort.text?.toString()?.trim()?.toIntOrNull() ?: 0
            val mtu = b.etMtu.text?.toString()?.trim()?.toIntOrNull() ?: 0
            if (port in 1024..65535 && mtu >= 1200 && mtu <= 1500) {
                Prefs.socksPort = port
                Prefs.mtu = mtu
                Toast.makeText(context, R.string.toast_saved, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, R.string.toast_invalid_core, Toast.LENGTH_SHORT).show()
            }
        }

        b.switchAutoConnect.setOnCheckedChangeListener { _, checked ->
            Prefs.autoConnect = checked
        }

        val themes = resources.getStringArray(R.array.theme_entries)
        val themeValues = resources.getStringArray(R.array.theme_values)
        b.spTheme.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, themes))
        val themeIdx = themeValues.indexOf(Prefs.themeMode)
        b.spTheme.setText(themes[if (themeIdx >= 0) themeIdx else 2], false)
        b.spTheme.setOnItemClickListener { _, _, position, _ ->
            Prefs.themeMode = themeValues.getOrNull(position) ?: "dark"
            (requireActivity().application as App).applyTheme()
        }

        val langs = resources.getStringArray(R.array.language_entries)
        val langValues = resources.getStringArray(R.array.language_values)
        b.spLanguage.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, langs))
        val langIdx = langValues.indexOf(Prefs.language)
        b.spLanguage.setText(langs[if (langIdx >= 0) langIdx else 0], false)
        b.spLanguage.setOnItemClickListener { _, _, position, _ ->
            Prefs.language = langValues.getOrNull(position) ?: ""
            (requireActivity().application as App).applyLanguage()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
