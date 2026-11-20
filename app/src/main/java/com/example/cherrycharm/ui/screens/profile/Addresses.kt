package com.example.cherrycharm.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.cherrycharm.core.Resource
import com.example.cherrycharm.core.isValidBdPhone
import com.example.cherrycharm.data.model.Address
import com.example.cherrycharm.data.repository.AddressRepository
import com.example.cherrycharm.data.repository.UserRepository
import com.example.cherrycharm.ui.components.*
import com.example.cherrycharm.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Bangladesh's 8 divisions + common delivery districts, for quick picking. */
private val DIVISIONS = listOf("Dhaka", "Chattogram", "Khulna", "Rajshahi", "Barishal", "Sylhet", "Rangpur", "Mymensingh")

@HiltViewModel
class AddressesViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: AddressRepository,
    private val users: UserRepository,
) : ViewModel() {
    val editId: String = savedState.get<String>("id").orEmpty()
    val messages = MessageQueue()
    val addresses: StateFlow<Resource<List<Address>>> = repo.addresses

    private val _editing = MutableStateFlow<Address?>(null)
    val editing = _editing.asStateFlow()
    private val _saving = MutableStateFlow(false)
    val saving = _saving.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = if (editId.isBlank()) null else
                (repo.addresses.filter { it is Resource.Success }.first() as? Resource.Success)?.data?.firstOrNull { it.id == editId }
            val profile = users.profile.value
            _editing.value = existing ?: Address(
                fullName = profile?.name.orEmpty(),
                phone = profile?.phone.orEmpty(),
                city = "Dhaka",
                state = "Dhaka",
            )
        }
    }

    fun save(a: Address, onDone: () -> Unit) {
        viewModelScope.launch {
            _saving.value = true
            when (val r = repo.save(a)) {
                is Resource.Success -> onDone()
                is Resource.Error -> messages.send(r.message)
                else -> Unit
            }
            _saving.value = false
        }
    }

    fun setDefault(a: Address) = viewModelScope.launch { repo.setDefault(a.id) }

    fun delete(a: Address) = viewModelScope.launch {
        when (val r = repo.delete(a.id)) {
            is Resource.Success -> messages.send("Address deleted", "Undo") { viewModelScope.launch { repo.save(a) } }
            is Resource.Error -> messages.send(r.message)
            else -> Unit
        }
    }
}

@Composable
fun AddressesScreen(navController: NavController, viewModel: AddressesViewModel = hiltViewModel()) {
    val res by viewModel.addresses.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    ShowMessages(viewModel.messages.flow, snackbar)

    Scaffold(
        topBar = { AppTopBar("Addresses", onBack = { navController.popBackStack() }) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Routes.addressEdit()) },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Add address") },
            )
        },
    ) { padding ->
        when (val r = res) {
            is Resource.Success -> if (r.data.isEmpty()) {
                EmptyState(
                    "📍", "No saved addresses", "Add one now so checkout takes seconds.",
                    modifier = Modifier.padding(padding),
                )
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(r.data, key = { it.id }) { a ->
                        AddressCard(
                            a,
                            onEdit = { navController.navigate(Routes.addressEdit(a.id)) },
                            onDefault = { viewModel.setDefault(a) },
                            onDelete = { viewModel.delete(a) },
                        )
                    }
                }
            }
            is Resource.Error -> ErrorState(r.message, onRetry = null, modifier = Modifier.padding(padding))
            else -> LoadingBox(Modifier.padding(padding))
        }
    }
}

private fun labelIcon(label: String) = when (label) {
    "Home" -> Icons.Filled.Home
    "Work" -> Icons.Filled.Work
    else -> Icons.Filled.Place
}

@Composable
private fun AddressCard(a: Address, onEdit: () -> Unit, onDefault: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Surface(onClick = onEdit, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(labelIcon(a.label), null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(a.label, style = MaterialTheme.typography.titleSmall)
                    if (a.isDefault) {
                        Spacer(Modifier.width(8.dp))
                        Pill("Default", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("${a.fullName} · ${a.phone}", style = MaterialTheme.typography.bodyMedium)
                Text(a.singleLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "More") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; onEdit() })
                    if (!a.isDefault) DropdownMenuItem(text = { Text("Set as default") }, onClick = { menu = false; onDefault() })
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { menu = false; onDelete() },
                    )
                }
            }
        }
    }
}

// ── Add / edit address ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddressEditScreen(navController: NavController, viewModel: AddressesViewModel = hiltViewModel()) {
    val initial by viewModel.editing.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    ShowMessages(viewModel.messages.flow, snackbar)
    val start = initial

    Scaffold(
        topBar = { AppTopBar(if (viewModel.editId.isBlank()) "New address" else "Edit address", onBack = { navController.popBackStack() }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (start == null) { LoadingBox(Modifier.padding(padding)); return@Scaffold }

        var label by rememberSaveable { mutableStateOf(start.label) }
        var fullName by rememberSaveable { mutableStateOf(start.fullName) }
        var phone by rememberSaveable { mutableStateOf(start.phone) }
        var line1 by rememberSaveable { mutableStateOf(start.line1) }
        var line2 by rememberSaveable { mutableStateOf(start.line2) }
        var city by rememberSaveable { mutableStateOf(start.city) }
        var district by rememberSaveable { mutableStateOf(start.state) }
        var postcode by rememberSaveable { mutableStateOf(start.pincode) }
        var isDefault by rememberSaveable { mutableStateOf(start.isDefault) }
        var submitted by remember { mutableStateOf(false) }

        val errors = mapOf(
            "name" to (fullName.trim().length < 2),
            "phone" to !phone.isValidBdPhone(),
            "line1" to (line1.trim().length < 5),
            "city" to city.isBlank(),
            "post" to (postcode.isNotBlank() && !postcode.matches(Regex("\\d{4}"))),
        )
        fun err(key: String, msg: String) = if (submitted && errors[key] == true) msg else null

        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Save as", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Home", "Work", "Other").forEach { l ->
                    FilterChip(
                        selected = label == l,
                        onClick = { label = l },
                        label = { Text(l) },
                        leadingIcon = { Icon(labelIcon(l), null, Modifier.size(18.dp)) },
                    )
                }
            }
            AppTextField(fullName, { fullName = it }, "Recipient name", leadingIcon = Icons.Filled.Person, error = err("name", "Enter a name"))
            AppTextField(
                phone, { if (it.length <= 14) phone = it }, "Mobile number", leadingIcon = Icons.Filled.Phone,
                keyboardType = KeyboardType.Phone, placeholder = "01XXXXXXXXX",
                error = err("phone", "Use an 11-digit number like 01XXXXXXXXX"),
            )
            AppTextField(
                line1, { line1 = it }, "House, road, block", leadingIcon = Icons.Filled.Home,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                error = err("line1", "Enter a full street address"),
            )
            AppTextField(
                line2, { line2 = it }, "Area / landmark (optional)", leadingIcon = Icons.Filled.Apartment,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
            Text("City / district", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DIVISIONS.forEach { d ->
                    FilterChip(selected = city.equals(d, true), onClick = { city = d; district = d }, label = { Text(d) })
                }
            }
            AppTextField(
                city, { city = it }, "City", leadingIcon = Icons.Filled.LocationCity,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                error = err("city", "Enter your city"),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppTextField(
                    district, { district = it }, "District", modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                AppTextField(
                    postcode, { if (it.length <= 4) postcode = it.filter(Char::isDigit) }, "Post code",
                    modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number,
                    error = err("post", "4 digits"),
                )
            }
            Text(
                if (city.trim().equals("Dhaka", true)) "Inside Dhaka · delivery in 1–2 days" else "Outside Dhaka · delivery in 3–5 days",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Use as default address", modifier = Modifier.weight(1f))
                Switch(checked = isDefault, onCheckedChange = { isDefault = it })
            }
            Spacer(Modifier.height(8.dp))
            PrimaryButton(
                "Save address",
                loading = saving,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    submitted = true
                    if (errors.values.none { it }) {
                        viewModel.save(
                            start.copy(
                                label = label, fullName = fullName.trim(), phone = phone.trim(), line1 = line1.trim(),
                                line2 = line2.trim(), city = city.trim(), state = district.trim(), pincode = postcode.trim(),
                                isDefault = isDefault,
                            )
                        ) { navController.popBackStack() }
                    }
                },
            )
        }
    }
}
