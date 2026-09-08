package com.onetowncity.app.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat

object OneTownCityColors {
    val Light: ColorScheme = lightColorScheme(
        primary = BrandNavy,
        onPrimary = Color.White,
        primaryContainer = PaleBlue,
        onPrimaryContainer = BrandNavy,
        secondary = BrandAmber,
        onSecondary = BrandNavy,
        secondaryContainer = WarmSand,
        onSecondaryContainer = BrandNavy,
        tertiary = BrandOrange,
        onTertiary = BrandNavy,
        background = AppBackground,
        onBackground = Ink,
        surface = SurfaceColor,
        onSurface = Ink,
        surfaceVariant = PanelSurface,
        onSurfaceVariant = InkSoft,
        outline = BorderSoft,
        outlineVariant = BorderStrong,
        error = ErrorRed,
        onError = Color.White,
        errorContainer = ErrorContainer,
        onErrorContainer = ErrorText,
        scrim = Color.Black.copy(alpha = 0.45f)
    )

    val Dark: ColorScheme = darkColorScheme(
        primary = BrandAmber,
        onPrimary = BrandNavy,
        primaryContainer = BrandNavy,
        onPrimaryContainer = Color.White,
        secondary = BrandBlue,
        onSecondary = Color.White,
        secondaryContainer = SlateBlue,
        onSecondaryContainer = Color.White,
        tertiary = BrandOrange,
        onTertiary = BrandNavy,
        background = DarkBackground,
        onBackground = Color.White,
        surface = DarkSurface,
        onSurface = Color.White,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = SoftGray,
        outline = DarkBorder,
        outlineVariant = DarkBorderStrong,
        error = ErrorRed,
        onError = Color.White,
        errorContainer = ErrorContainerDark,
        onErrorContainer = Color(0xFFFFDAD6),
        scrim = Color.Black.copy(alpha = 0.60f)
    )

    val BrandGradient: Brush = Brush.linearGradient(
        colors = listOf(BrandOrange, BrandAmber, BrandOrangeDark)
    )
}

object OneTownCityTypography {
    private val defaultPlatformTextStyle = PlatformTextStyle(includeFontPadding = false)

    private fun textStyle(
        fontSize: TextUnit,
        fontWeight: FontWeight = FontWeight.Normal,
        lineHeight: TextUnit = 1.4.sp,
        letterSpacing: TextUnit = 0.sp,
        textAlign: TextAlign = TextAlign.Start,
    ): TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = fontWeight,
        fontSize = fontSize,
        lineHeight = lineHeight,
        letterSpacing = letterSpacing,
        textAlign = textAlign,
        platformStyle = defaultPlatformTextStyle,
    )

    val default = Typography(
        displayLarge = textStyle(32.sp, FontWeight.Bold, 40.sp, (-0.02).sp),
        displayMedium = textStyle(28.sp, FontWeight.Bold, 36.sp, (-0.02).sp),
        displaySmall = textStyle(24.sp, FontWeight.Bold, 32.sp, (-0.02).sp),
        headlineLarge = textStyle(22.sp, FontWeight.Bold, 28.sp, (-0.01).sp),
        headlineMedium = textStyle(20.sp, FontWeight.SemiBold, 26.sp, (-0.01).sp),
        headlineSmall = textStyle(18.sp, FontWeight.SemiBold, 24.sp),
        titleLarge = textStyle(18.sp, FontWeight.SemiBold, 24.sp),
        titleMedium = textStyle(16.sp, FontWeight.SemiBold, 22.sp),
        titleSmall = textStyle(14.sp, FontWeight.SemiBold, 18.sp),
        bodyLarge = textStyle(16.sp, FontWeight.Normal, 24.sp),
        bodyMedium = textStyle(14.sp, FontWeight.Normal, 20.sp),
        bodySmall = textStyle(12.sp, FontWeight.Normal, 16.sp),
        labelLarge = textStyle(14.sp, FontWeight.Medium, 20.sp),
        labelMedium = textStyle(12.sp, FontWeight.Medium, 16.sp),
        labelSmall = textStyle(11.sp, FontWeight.Medium, 14.sp),
    )

    val textScaleAware = TextScaleAwareDefaults
}

object OneTownCitySpacing {
    val none = 0.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val huge = 40.dp
    val giant = 48.dp
}

object OneTownCityCornerRadii {
    val none = 0.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val pill = 999.dp
}

object OneTownCityElevation {
    val none = 0.dp
    val low = 2.dp
    val medium = 6.dp
    val high = 12.dp
    val large = 20.dp
}

object OneTownCityBorders {
    val thin = 1.dp
    val medium = 1.5.dp
}

object OneTownCityIcons {
    val home = Icons.Outlined.Home
    val homeFilled = Icons.Filled.Home
    val search = Icons.Outlined.Search
    val searchFilled = Icons.Filled.Search
    val favorite = Icons.Outlined.FavoriteBorder
    val location = Icons.Filled.LocationOn
    val profile = Icons.Outlined.Person
    val settings = Icons.Filled.Settings
    val error = Icons.Filled.Error
    val warning = Icons.Filled.Info
    val star = Icons.Filled.Star
    val check = Icons.Filled.Check
    val close = Icons.Filled.Close
    val back = Icons.AutoMirrored.Filled.ArrowBack
    val sad = Icons.Outlined.SentimentDissatisfied
}

object OneTownCityButtonDefaults {
    private val minTouchTarget = 48.dp

    val minHeight = minTouchTarget
}

enum class OneTownCityButtonVariant {
    Primary,
    Secondary,
    Outlined,
    Text,
}

@Composable
fun OneTownCityButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    variant: OneTownCityButtonVariant = OneTownCityButtonVariant.Primary,
) {
    val colors = when (variant) {
        OneTownCityButtonVariant.Primary -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.66f),
        )
        OneTownCityButtonVariant.Secondary -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.38f),
            disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.66f),
        )
        OneTownCityButtonVariant.Outlined -> ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )
        OneTownCityButtonVariant.Text -> ButtonDefaults.textButtonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )
    }

    val shape = RoundedCornerShape(OneTownCityCornerRadii.md)

    val adaptiveModifier = modifier
        .defaultMinSize(minHeight = OneTownCityButtonDefaults.minHeight)

    when (variant) {
        OneTownCityButtonVariant.Outlined -> {
            OutlinedButton(
                onClick = onClick,
                modifier = adaptiveModifier,
                enabled = enabled,
                shape = shape,
                colors = colors,
                contentPadding = ButtonDefaults.ContentPadding,
            ) {
                ButtonContent(text, leadingIcon, trailingIcon)
            }
        }
        OneTownCityButtonVariant.Text -> {
            TextButton(
                onClick = onClick,
                modifier = adaptiveModifier,
                enabled = enabled,
                shape = shape,
                colors = colors,
                contentPadding = ButtonDefaults.ContentPadding,
            ) {
                ButtonContent(text, leadingIcon, trailingIcon)
            }
        }
        else -> {
            Button(
                onClick = onClick,
                modifier = adaptiveModifier,
                enabled = enabled,
                shape = shape,
                colors = colors,
                contentPadding = ButtonDefaults.ContentPadding,
            ) {
                ButtonContent(text, leadingIcon, trailingIcon)
            }
        }
    }
}

@Composable
private fun ButtonContent(
    text: String,
    leadingIcon: ImageVector?,
    trailingIcon: ImageVector?,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OneTownCitySpacing.xs),
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelLarge,
        )
        if (trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
fun OneTownCityCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(OneTownCityCornerRadii.md),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = OneTownCityElevation.low),
    ) {
        Box(
            modifier = Modifier
                .padding(OneTownCitySpacing.lg),
        ) {
            content()
        }
    }
}

@Composable
fun OneTownCityChip(
    text: String,
    selected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, style = MaterialTheme.typography.labelLarge) },
        modifier = modifier,
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = OneTownCityIcons.check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
            selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
            enabled = true,
            selected = selected,
        ),
    )
}

@Composable
fun OneTownCityTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth(),
        label = if (label != null) {
            { Text(label) }
        } else null,
        placeholder = if (placeholder != null) {
            { Text(placeholder) }
        } else null,
        leadingIcon = if (leadingIcon != null) {
            {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                )
            }
        } else null,
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        enabled = enabled,
        shape = RoundedCornerShape(OneTownCityCornerRadii.md),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

@Composable
fun OneTownCitySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    leadingIcon: ImageVector = OneTownCityIcons.search,
    trailing: @Composable (() -> Unit)? = null,
    onClear: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        shape = RoundedCornerShape(OneTownCityCornerRadii.xl),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = OneTownCityElevation.low,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = OneTownCitySpacing.md, vertical = OneTownCitySpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(OneTownCitySpacing.sm))
            Box(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        innerTextField()
                    },
                )
            }
            if (query.isNotEmpty() && onClear != null) {
                IconButton(onClick = onClear) {
                    Icon(imageVector = OneTownCityIcons.close, contentDescription = null)
                }
            }
            if (trailing != null) {
                trailing()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneTownCityTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    onNavigationClick: (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        modifier = modifier,
        navigationIcon = {
            if (navigationIcon != null && onNavigationClick != null) {
                IconButton(onClick = onNavigationClick) {
                    Icon(
                        imageVector = navigationIcon,
                        contentDescription = null,
                    )
                }
            }
        },
        actions = {
            if (actions != null) actions()
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@Composable
fun OneTownCityBottomNavigation(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    items: List<OneTownCityBottomNavItem>,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                icon = {
                    Icon(
                        imageVector = if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                    )
                },
                label = { Text(item.label, style = MaterialTheme.typography.labelMedium) },
                alwaysShowLabel = true,
            )
        }
    }
}

data class OneTownCityBottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

@Composable
fun OneTownCityDialog(
    onDismissRequest: () -> Unit,
    title: String,
    text: String,
    confirmText: String = "OK",
    dismissText: String? = null,
    onConfirm: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = true),
    ) {
        Surface(
            shape = RoundedCornerShape(OneTownCityCornerRadii.xl),
            tonalElevation = OneTownCityElevation.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(0.92f),
        ) {
            Column(
                modifier = Modifier.padding(OneTownCitySpacing.xl),
                verticalArrangement = Arrangement.spacedBy(OneTownCitySpacing.md),
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (dismissText != null) {
                        TextButton(
                            onClick = {
                                onDismiss?.invoke()
                                onDismissRequest()
                            },
                        ) {
                            Text(dismissText)
                        }
                    }
                    Spacer(modifier = Modifier.width(OneTownCitySpacing.sm))
                    OneTownCityButton(
                        text = confirmText,
                        onClick = {
                            onConfirm?.invoke()
                            onDismissRequest()
                        },
                        variant = OneTownCityButtonVariant.Primary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneTownCityBottomSheet(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = OneTownCityCornerRadii.xl, topEnd = OneTownCityCornerRadii.xl),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        content()
    }
}

@Composable
fun OneTownCityCircularLoading(
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(OneTownCitySpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(40.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp,
        )
        if (label != null) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun OneTownCityEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = OneTownCityIcons.sad,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(OneTownCitySpacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f),
            shape = CircleShape,
            modifier = Modifier.size(72.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
        Spacer(Modifier.height(OneTownCitySpacing.lg))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(OneTownCitySpacing.sm))
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(OneTownCitySpacing.lg))
            action()
        }
    }
}

@Composable
fun OneTownCityErrorState(
    title: String = "Something went wrong",
    message: String = "Please try again later.",
    modifier: Modifier = Modifier,
    actionText: String = "Retry",
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(OneTownCitySpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(OneTownCitySpacing.md),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = CircleShape,
            modifier = Modifier.size(64.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = OneTownCityIcons.error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            OneTownCityButton(
                text = actionText,
                onClick = onRetry,
                variant = OneTownCityButtonVariant.Primary,
            )
        }
    }
}

@Composable
fun OneTownCityImageContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(OneTownCityCornerRadii.lg))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = OneTownCityBorders.thin,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                shape = RoundedCornerShape(OneTownCityCornerRadii.lg),
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun OneTownCityListItem(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val clickModifier = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    Surface(
        modifier = modifier.then(clickModifier),
        shape = RoundedCornerShape(OneTownCityCornerRadii.md),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OneTownCitySpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OneTownCitySpacing.md),
        ) {
            if (leading != null) {
                leading()
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(OneTownCitySpacing.xs))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (trailing != null) {
                trailing()
            }
        }
    }
}

@Composable
fun OneTownCitySectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (trailing != null) trailing()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OneTownCityChipGroup(
    items: List<String>,
    selected: Set<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(OneTownCitySpacing.sm),
        verticalArrangement = Arrangement.spacedBy(OneTownCitySpacing.sm),
    ) {
        items.forEach { item ->
            OneTownCityChip(
                text = item,
                selected = selected.contains(item),
                onClick = { onSelected(item) },
            )
        }
    }
}

@Composable
fun OneTownCityResponsiveColumn(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(OneTownCitySpacing.md),
    ) {
        content()
    }
}

@Composable
fun OneTownCityAdaptiveSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(OneTownCityCornerRadii.lg),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = OneTownCityElevation.low,
    ) {
        content()
    }
}

@Immutable
public data class ScaleAwareTextSizes(
    val body: TextStyle,
    val title: TextStyle,
    val heading: TextStyle,
)

object TextScaleAwareDefaults {
    /**
     * Returns the theme's typography styles unmodified. `sp`-based font
     * sizes (every MaterialTheme.typography style) already scale correctly
     * with the system accessibility "Font size" setting on their own —
     * that's what `sp` (scale-independent pixels) means. A previous version
     * of this function re-multiplied the font size by
     * `LocalDensity.current.density`, which is the screen's *physical pixel
     * density* (1.0-4.0 depending on the device), not the font-scale
     * setting (`LocalDensity.current.fontScale`) — on a typical xxhdpi
     * phone (density 3.0) that inflated body text to ~3x its intended
     * size. Multiplying by the correct `fontScale` property instead would
     * still be wrong: it would double-apply the scaling, since `sp` values
     * are already resolved against `fontScale` when Compose measures text.
     * No manual scaling belongs here at all.
     */
    @Composable
    fun values(): ScaleAwareTextSizes {
        return ScaleAwareTextSizes(
            body = MaterialTheme.typography.bodyLarge,
            title = MaterialTheme.typography.titleLarge,
            heading = MaterialTheme.typography.headlineLarge,
        )
    }
}

@Composable
fun OneTownCityTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) OneTownCityColors.Dark else OneTownCityColors.Light
    val typography = OneTownCityTypography.default
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content,
    )
}

@Composable
fun OneTownCityScreenContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun OneTownCityWindowSizeAware(
    activity: androidx.activity.ComponentActivity,
    content: @Composable (WindowWidthSizeClass) -> Unit,
) {
    val widthClass = calculateWindowSizeClass(activity = activity).widthSizeClass
    content(widthClass)
}

private val BrandNavy = Color(0xFF020617)
private val BrandAmber = Color(0xFFF0A93A)
private val BrandOrange = Color(0xFFF97316)
private val BrandOrangeDark = Color(0xFFEA580C)
private val BrandBlue = Color(0xFF1F63A6)
private val BrandSlate = Color(0xFF0B0D16)
private val AppBackground = Color(0xFFF7F8FB)
private val SurfaceColor = Color(0xFFFFFFFF)
private val PanelSurface = Color(0xFFF2F5FA)
private val Ink = Color(0xFF14172A)
private val InkSoft = Color(0xFF5B607A)
private val BorderSoft = Color(0xFFE2E5EF)
private val BorderStrong = Color(0xFFCBD0DF)
private val WarmSand = Color(0xFFFDF1DD)
private val PaleBlue = Color(0xFFDCE8F6)
private val ErrorRed = Color(0xFFDC3545)
private val ErrorContainer = Color(0xFFF8D7DA)
private val ErrorText = Color(0xFF7F1D1D)
private val DarkBackground = Color(0xFF020617)
private val DarkSurface = Color(0xFF12131B)
private val DarkSurfaceVariant = Color(0xFF191B26)
private val DarkBorder = Color(0xFF2A2E3B)
private val DarkBorderStrong = Color(0xFF3E4258)
private val SlateBlue = Color(0xFF1E293B)
private val SoftGray = Color(0xFFB9C0D3)
private val ErrorContainerDark = Color(0xFF5D1F26)
