package com.example.smartcivicgovernance.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartcivicgovernance.databinding.FragmentUserManagementBinding
import com.example.smartcivicgovernance.ui.adapter.UserAdapter

class UserManagementFragment : Fragment() {

    private var _binding: FragmentUserManagementBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminViewModel by viewModels()
    private lateinit var userAdapter: UserAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentUserManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()

        binding.swipeRefreshUsers.setOnRefreshListener {
            viewModel.loadAllUsers()
        }

        viewModel.loadAllUsers()
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdapter { user, enabled ->
            viewModel.setUserAccountStatus(user.uid, enabled)
        }
        binding.rvUsersList.layoutManager = LinearLayoutManager(context)
        binding.rvUsersList.adapter = userAdapter
    }

    private fun setupObservers() {
        viewModel.users.observe(viewLifecycleOwner) { users ->
            userAdapter.submitList(users)
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefreshUsers.isRefreshing = isLoading
        }

        viewModel.actionSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(context, "User status updated successfully", Toast.LENGTH_SHORT).show()
                viewModel.loadAllUsers()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg != null) {
                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
