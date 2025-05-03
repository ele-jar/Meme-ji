package com.elejar.memeji.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.elejar.memeji.R
import com.elejar.memeji.databinding.FragmentSettingsBinding
import com.elejar.memeji.util.PreferencesHelper
import com.elejar.memeji.viewmodel.MemeViewModel

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MemeViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = insets.top, bottom = insets.bottom)
            WindowInsetsCompat.CONSUMED
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSwitch()
        observeViewModel()
    }

    private fun setupSwitch() {
        binding.switchCutieModeSettings.isChecked = PreferencesHelper.isCutieModeEnabled(requireContext())
        binding.switchCutieModeSettings.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateCutieMode(isChecked)
        }
    }

    private fun observeViewModel() {
        viewModel.isCutieModeEnabled.observe(viewLifecycleOwner) { isEnabled ->
            if (binding.switchCutieModeSettings.isChecked != isEnabled) {
                binding.switchCutieModeSettings.isChecked = isEnabled
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
