package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.PhonixCyan
import com.example.ui.theme.PhonixGreen
import com.example.ui.theme.PhonixRed
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.viewmodel.PhonixUiState
import com.example.viewmodel.PhonixViewModel

/**
 * Renames one desktop entry or taskbar pin. The id is shared by the desktop icon, the bar entry
 * and the Start menu tile, so a single rename updates every surface that shows it while the
 * designed artwork stays exactly as it was.
 */
@androidx.compose.runtime.Composable
fun RenameEntryDialog(uiState: PhonixUiState, viewModel: PhonixViewModel) {
    val id = uiState.renameTargetId ?: return
    val isPinned = uiState.customization.taskbarPins.any { it.id == id }
    val isOnDesktop = uiState.customization.shortcuts.any { it.id == id }
    val focus = remember { FocusRequester() }

    LaunchedEffect(id) { runCatching { focus.requestFocus() } }

    Dialog(onDismissRequest = { viewModel.cancelRename() }) {
        Surface(color = Slate950, shape = RoundedCornerShape(14.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Rename", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { viewModel.cancelRename() }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Slate400, modifier = Modifier.size(16.dp))
                    }
                }

                Text(
                    buildString {
                        append("The label shown under the icon. ")
                        append(
                            when {
                                isOnDesktop && isPinned -> "Applies to the desktop icon and the taskbar entry."
                                isOnDesktop -> "Applies to the desktop icon."
                                else -> "Applies to the taskbar entry."
                            }
                        )
                        append(" The icon artwork does not change.")
                    },
                    color = Slate400,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )

                OutlinedTextField(
                    value = uiState.renameDraft,
                    onValueChange = { viewModel.updateRenameDraft(it) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus)
                        .testTag("rename_field"),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Slate900,
                        unfocusedContainerColor = Slate900,
                        focusedBorderColor = PhonixCyan,
                        unfocusedBorderColor = Slate800
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = { viewModel.commitRename() }
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isPinned) {
                        OutlinedButton(
                            onClick = {
                                viewModel.unpinFromTaskbar(id)
                                viewModel.cancelRename()
                            },
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Unpin from taskbar", color = PhonixRed, fontSize = 10.sp)
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { viewModel.cancelRename() }, modifier = Modifier.height(36.dp)) {
                        Text("Cancel", color = Slate300, fontSize = 11.sp)
                    }
                    Button(
                        onClick = { viewModel.commitRename() },
                        colors = ButtonDefaults.buttonColors(containerColor = PhonixGreen),
                        modifier = Modifier.height(36.dp).testTag("rename_save")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Save", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
