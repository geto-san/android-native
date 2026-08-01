package com.wildwatch.app.feature.report.dynamic.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wildwatch.app.core.database.IncidentType
import com.wildwatch.app.core.ui.component.BackHeader
import com.wildwatch.app.feature.report.dynamic.DynamicReportUiState
import com.wildwatch.app.feature.report.dynamic.DynamicReportViewModel
import com.wildwatch.app.feature.report.dynamic.model.*
import kotlinx.coroutines.launch

@Composable
fun DynamicReportScreen(
    type: IncidentType,
    onBack: () -> Unit,
    onSubmitted: (String) -> Unit,
    onNavigateToCamera: (String?) -> Unit,
    draftId: String? = null,
    viewModel: DynamicReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(type, draftId) {
        val questions = FormSchemaLoader.loadQuestions(context, type)
            ?: when (type) {
                IncidentType.SIGHTING -> FormSchemas.SightingForm
                IncidentType.CONFLICT -> FormSchemas.ConflictForm
                else -> emptyList()
            }
        viewModel.initialize(type, questions, draftId)
    }

    LaunchedEffect(uiState.savedIncidentId) {
        uiState.savedIncidentId?.let {
            viewModel.consumeSavedEvent()
            onSubmitted(it)
        }
    }

    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (uiState.answers.isNotEmpty() && !uiState.isSaving) {
            showExitDialog = true
        } else {
            onBack()
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Save as draft?") },
            text = { Text("You have unsaved changes. Would you like to save this report as a draft and finish it later?") },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.save(asDraft = true)
                    showExitDialog = false
                    onBack()
                }) {
                    Text("Save Draft")
                }
            },
            dismissButton = {
                TextButton(onClick = { onBack() }) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            BackHeader(
                title = if (type == IncidentType.SIGHTING) "New Sighting" else "Conflict Report",
                subtitle = "Dynamic Form",
                onBack = { 
                    if (uiState.answers.isNotEmpty()) showExitDialog = true else onBack() 
                },
                actions = {
                    TextButton(onClick = { viewModel.save(asDraft = true) }) {
                        Text("Save Draft", style = MaterialTheme.typography.labelMedium)
                    }
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(
                            imageVector = if (uiState.viewMode == FormViewMode.FLOW) Icons.Default.ViewAgenda else Icons.Default.ViewStream,
                            contentDescription = "Switch Mode"
                        )
                    }
                }
            )

            Box(modifier = Modifier.weight(1f)) {
                if (uiState.viewMode == FormViewMode.FLOW) {
                    FlowFormRenderer(
                        uiState = uiState,
                        onAnswerChanged = { id, ans -> viewModel.updateAnswer(id, ans) },
                        onAddPhoto = { onNavigateToCamera(null) },
                        onSubmit = { viewModel.save() }
                    )
                } else {
                    PagingFormRenderer(
                        uiState = uiState,
                        onAnswerChanged = { id, ans -> viewModel.updateAnswer(id, ans) },
                        onAddPhoto = { onNavigateToCamera(null) },
                        onSubmit = { viewModel.save() }
                    )
                }
            }
        }
    }
}

@Composable
fun FlowFormRenderer(
    uiState: DynamicReportUiState,
    onAnswerChanged: (String, Any?) -> Unit,
    onAddPhoto: () -> Unit,
    onSubmit: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        items(uiState.visibleQuestions, key = { it.id }) { question ->
            QuestionItem(
                question = question,
                answer = uiState.answers[question.id],
                onAnswerChanged = { onAnswerChanged(question.id, it) },
                onAddPhoto = onAddPhoto
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = uiState.canSubmit && !uiState.isSaving
            ) {
                Text(if (uiState.isSaving) "Submitting..." else "Submit Report", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PagingFormRenderer(
    uiState: DynamicReportUiState,
    onAnswerChanged: (String, Any?) -> Unit,
    onAddPhoto: () -> Unit,
    onSubmit: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { uiState.visibleQuestions.size + 1 })
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        LinearProgressIndicator(
            progress = { (pagerState.currentPage.toFloat() / uiState.visibleQuestions.size).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            userScrollEnabled = false // We control it with buttons
        ) { page ->
            if (page < uiState.visibleQuestions.size) {
                val question = uiState.visibleQuestions[page]
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.TopStart) {
                    QuestionItem(
                        question = question,
                        answer = uiState.answers[question.id],
                        onAnswerChanged = { onAnswerChanged(question.id, it) },
                        onAddPhoto = onAddPhoto
                    )
                }
            } else {
                // Summary/Submit Page
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Ready to submit?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Please review your answers before sharing with the community.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = onSubmit,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = uiState.canSubmit && !uiState.isSaving
                    ) {
                        Text(if (uiState.isSaving) "Submitting..." else "Submit Report", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                enabled = pagerState.currentPage > 0
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = null)
                Text("Previous")
            }

            Text(
                "Step ${pagerState.currentPage + 1} of ${uiState.visibleQuestions.size + 1}",
                style = MaterialTheme.typography.labelMedium
            )

            Button(
                onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                enabled = pagerState.currentPage < uiState.visibleQuestions.size && 
                          isQuestionFilled(uiState.visibleQuestions[pagerState.currentPage], uiState.answers)
            ) {
                Text("Next")
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
    }
}

private fun isQuestionFilled(question: Question, answers: Map<String, Any?>): Boolean {
    if (!question.isRequired) return true
    val ans = answers[question.id]
    return when (question.type) {
        QuestionType.PHOTOS -> (ans as? List<*>)?.isNotEmpty() ?: false
        else -> ans?.toString()?.isNotBlank() ?: false
    }
}
