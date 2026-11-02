package com.amitabhtours.invoice.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amitabhtours.invoice.R
import com.amitabhtours.invoice.domain.Fmt
import com.amitabhtours.invoice.ui.theme.extra
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * A field that remembers the previous invoice's value as grey text ([hint]).
 * If the driver does not type anything, the grey value is used. [cleared] = deliberately blank.
 */
@Immutable
data class MemField(val text: String = "", val hint: String = "", val cleared: Boolean = false) {
    val value: String get() = if (text.isNotEmpty()) text else if (cleared) "" else hint
    val usingHint: Boolean get() = text.isEmpty() && !cleared && hint.isNotEmpty()
    fun typed(t: String) = copy(text = t)
}

@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun MemoryField(
    label: String,
    field: MemField,
    onChange: (MemField) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Words,
    singleLine: Boolean = true,
    minLines: Int = 1,
    leading: ImageVector? = null,
    prefix: String? = null,
    suffix: String? = null,
    placeholder: String? = null,
    supporting: String? = null,
) {
    Column(modifier) {
        FieldLabel(label)
        val hintColor = extra.hint
        OutlinedTextField(
            value = field.text,
            onValueChange = { onChange(field.copy(text = it)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            minLines = minLines,
            shape = MaterialTheme.shapes.medium,
            placeholder = {
                val ph = when {
                    field.usingHint -> field.hint
                    else -> placeholder ?: ""
                }
                Text(ph, color = hintColor, maxLines = if (singleLine) 1 else 4, overflow = TextOverflow.Ellipsis)
            },
            leadingIcon = leading?.let { { Icon(it, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) } },
            prefix = prefix?.let { { Text(it) } },
            suffix = suffix?.let { { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
            trailingIcon = when {
                field.text.isNotEmpty() -> ({
                    IconButton(onClick = { onChange(field.copy(text = "")) }) { Icon(Icons.Rounded.Close, "Clear") }
                })
                field.usingHint -> ({
                    IconButton(onClick = { onChange(field.copy(cleared = true)) }) { Icon(Icons.Rounded.Close, "Leave blank") }
                })
                field.cleared && field.hint.isNotEmpty() -> ({
                    IconButton(onClick = { onChange(field.copy(cleared = false)) }) {
                        Icon(Icons.AutoMirrored.Rounded.Undo, "Use previous value", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                })
                else -> null
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, capitalization = capitalization, imeAction = if (singleLine) androidx.compose.ui.text.input.ImeAction.Next else androidx.compose.ui.text.input.ImeAction.Default),
            colors = fieldColors(),
            supportingText = supporting?.let { { Text(it) } },
        )
    }
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    unfocusedBorderColor = extra.cardBorder,
)

@Composable
fun PlainField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    placeholder: String = "",
    prefix: String? = null,
    suffix: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    supporting: String? = null,
    leading: ImageVector? = null,
    isError: Boolean = false,
) {
    Column(modifier) {
        if (label.isNotEmpty()) FieldLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            minLines = minLines,
            shape = MaterialTheme.shapes.medium,
            placeholder = { Text(placeholder, color = extra.hint) },
            prefix = prefix?.let { { Text(it) } },
            suffix = suffix?.let { { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
            leadingIcon = leading?.let { { Icon(it, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) } },
            trailingIcon = if (value.isNotEmpty()) ({ IconButton(onClick = { onChange("") }) { Icon(Icons.Rounded.Close, "Clear") } }) else null,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, capitalization = capitalization),
            colors = fieldColors(),
            supportingText = supporting?.let { { Text(it) } },
            isError = isError,
        )
    }
}

/** Money input: digits and one dot. */
fun moneyFilter(s: String): String {
    val cleaned = s.filter { it.isDigit() || it == '.' }
    val firstDot = cleaned.indexOf('.')
    return if (firstDot < 0) cleaned.take(9) else (cleaned.substring(0, firstDot + 1) + cleaned.substring(firstDot + 1).replace(".", "").take(2)).take(12)
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    container: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    border: Color? = extra.cardBorder,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    val colors = CardDefaults.cardColors(containerColor = container)
    val b = border?.let { BorderStroke(1.dp, it) }
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, border = b) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    } else {
        Card(modifier = modifier, shape = shape, colors = colors, border = b) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    AppCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) { Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp)) }
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            action?.invoke()
        }
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        title = {
            Column {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleLarge)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        navigationIcon = {
            if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
        },
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BigTopBar(title: String, scrollBehavior: TopAppBarScrollBehavior, actions: @Composable RowScope.() -> Unit = {}) {
    LargeTopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    )
}

@Composable
fun AppLogo(size: Dp = 40.dp, modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.ic_logo), contentDescription = "Amitabh Tours & Travels logo", modifier = modifier.size(size))
}

@Composable
fun InfoBanner(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Info,
    container: Color = MaterialTheme.colorScheme.secondaryContainer,
    content: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    action: (@Composable () -> Unit)? = null,
) {
    Surface(color = container, shape = MaterialTheme.shapes.medium, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = content, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(text, color = content, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            action?.invoke()
        }
    }
}

@Composable
fun StatusPill(text: String, container: Color, content: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(CircleShape).background(container).padding(horizontal = 10.dp, vertical = 4.dp),
    ) { Text(text, color = content, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, sub: String? = null, icon: ImageVector? = null, accent: Color = MaterialTheme.colorScheme.primary) {
    AppCard(modifier, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(6.dp))
        // Big amounts shrink to fit the tile instead of being cut off.
        androidx.compose.foundation.text.BasicText(
            value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
            maxLines = 1,
            autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = MaterialTheme.typography.titleLarge.fontSize),
        )
        if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChoiceChips(options: List<T>, selected: T?, onSelect: (T) -> Unit, label: (T) -> String, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            FilterChip(
                selected = o == selected,
                onClick = { onSelect(o) },
                label = { Text(label(o), modifier = Modifier.padding(vertical = 8.dp)) },
                shape = MaterialTheme.shapes.small,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

@Composable
fun Stepper(value: String, onMinus: () -> Unit, onPlus: () -> Unit, modifier: Modifier = Modifier, unit: String = "") {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = onMinus, modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.Remove, "Less") }
        Text(
            if (unit.isEmpty()) value else "$value $unit",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(96.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        FilledTonalIconButton(onClick = onPlus, modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.Add, "More") }
    }
}

@Composable
fun KeyValue(label: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false, strong: Boolean = false) {
    Row(modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.42f))
        Text(
            value.ifBlank { "—" },
            style = if (strong) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (strong) FontWeight.Bold else FontWeight.Medium,
            color = if (highlight) extra.warning else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.58f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismiss: String = "Cancel",
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirm, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismiss) } },
    )
}

@Composable
fun EmptyState(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(34.dp)) }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}

/** Date + time picker as one field, with a "Now" shortcut. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeField(label: String, value: Long?, onChange: (Long?) -> Unit, modifier: Modifier = Modifier) {
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf<Long?>(null) }
    Column(modifier) {
        FieldLabel(label, trailing = {
            Text(
                "Now",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clip(MaterialTheme.shapes.small).clickable { onChange(System.currentTimeMillis()) }.padding(horizontal = 8.dp, vertical = 2.dp),
            )
        })
        Surface(
            onClick = { pickDate = true },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(1.dp, extra.cardBorder),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CalendarMonth, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text(
                    value?.let { Fmt.dateTime(it) } ?: "Tap to set",
                    color = if (value == null) extra.hint else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                if (value != null) {
                    Icon(Icons.Rounded.Close, "Clear", modifier = Modifier.clip(CircleShape).clickable { onChange(null) }.padding(2.dp))
                }
            }
        }
    }
    if (pickDate) {
        val initial = value ?: System.currentTimeMillis()
        val initialUtcMidnight = Fmt.localDate(initial).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = initialUtcMidnight)
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickDate = false
                    pickTime = state.selectedDateMillis ?: initialUtcMidnight
                }) { Text("Next") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
    pickTime?.let { dateUtc ->
        val base = value?.let { Fmt.ldt(it) } ?: LocalDateTime.now()
        val ts = rememberTimePickerState(initialHour = base.hour, initialMinute = base.minute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { pickTime = null },
            title = { Text("Choose time") },
            text = { TimePicker(state = ts) },
            confirmButton = {
                TextButton(onClick = {
                    val date = Instant.ofEpochMilli(dateUtc).atZone(ZoneOffset.UTC).toLocalDate()
                    val ms = date.atTime(ts.hour, ts.minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    onChange(ms)
                    pickTime = null
                }) { Text("Done") }
            },
            dismissButton = { TextButton(onClick = { pickTime = null }) { Text("Cancel") } },
        )
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun IconCircle(icon: ImageVector, container: Color, content: Color, size: Dp = 40.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(container), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = content, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(extra.cardBorder))
}

@Composable
fun BorderBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.border(1.dp, extra.cardBorder, MaterialTheme.shapes.medium)) { content() }
}

val BigNumber = 28.sp
