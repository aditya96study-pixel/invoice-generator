package com.amitabhtours.invoice.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.amitabhtours.invoice.data.InvoiceEntity
import com.amitabhtours.invoice.domain.Fmt
import com.amitabhtours.invoice.domain.Money
import com.amitabhtours.invoice.ui.theme.extra

fun initials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "#"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts[0].take(1) + parts.last().take(1)).uppercase()
    }
}

@Composable
fun Avatar(name: String, size: Int = 44, container: Color = MaterialTheme.colorScheme.primaryContainer, content: Color = MaterialTheme.colorScheme.onPrimaryContainer) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(container), contentAlignment = Alignment.Center) {
        Text(initials(name), color = content, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun InvoiceStatusPill(inv: InvoiceEntity) {
    when {
        inv.isCancelled -> StatusPill("Cancelled", extra.dangerContainer, extra.danger)
        inv.outstandingPaise > 0 -> StatusPill("Due ${Money.short(inv.outstandingPaise)}", extra.warningContainer, extra.warning)
        else -> StatusPill("Paid", extra.successContainer, extra.success)
    }
}

@Composable
fun InvoiceRow(inv: InvoiceEntity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(inv.customerName.ifBlank { inv.number.takeLast(2) })
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                inv.customerName.ifBlank { "Customer not named" },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${inv.number} · ${Fmt.date(inv.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                Money.short(inv.totalPaise),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textDecoration = if (inv.isCancelled) TextDecoration.LineThrough else null,
            )
            Spacer(Modifier.height(4.dp))
            InvoiceStatusPill(inv)
        }
    }
}
