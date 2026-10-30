package com.propslab.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.propslab.PropsLabApp
import com.propslab.agent.ToolRegistry
import com.propslab.data.PropsRepository
import com.propslab.mathx.LocalMathModel

@Composable
fun rememberRepo(): PropsRepository = (LocalContext.current.applicationContext as PropsLabApp).repo

@Composable
fun rememberModel(): LocalMathModel = (LocalContext.current.applicationContext as PropsLabApp).mathModel

@Composable
fun rememberTools(): ToolRegistry = (LocalContext.current.applicationContext as PropsLabApp).agentTools
