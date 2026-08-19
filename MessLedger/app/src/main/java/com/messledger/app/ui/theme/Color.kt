package com.messledger.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Canonical color tokens from index.html :root ──────────────────────────
// These are the app's actual design tokens. Every screen should use these,
// not Material 3 baseline colors.

// Greens (brand)
val LedgerGreen = Color(0xFF1F3D34)       // --ledger-green
val LedgerGreenLight = Color(0xFF2F5647)  // --ledger-green-light
val LedgerGreenDark = Color(0xFF152A24)   // --ledger-green-dark

// Paper palette (backgrounds / surfaces)
val Paper = Color(0xFFF5F0E1)             // --paper
val PaperWhite = Color(0xFFFFFDF7)        // --paper-white
val PaperLine = Color(0xFFDED5B9)         // --paper-line

// Ink (text)
val Ink = Color(0xFF2A2620)               // --ink
val InkSoft = Color(0xFF726A57)           // --ink-soft

// Debit / Credit
val DebitRed = Color(0xFF9C3B2E)          // --debit-red
val DebitRedBg = Color(0xFFF4E4DF)        // --debit-red-bg
val CreditGreen = Color(0xFF3C6E4F)       // --credit-green
val CreditGreenBg = Color(0xFFE4EEE4)     // --credit-green-bg

// Brass (accents / highlights)
val Brass = Color(0xFFB8863A)             // --brass
val BrassBg = Color(0xFFF1E4C8)           // --brass-bg

// ── Legacy tokens (still referenced by existing screens not yet ported) ───
// These will be removed as each screen is ported to use the canonical tokens.
val LedgerGreenSoft = Color(0xFF4A9D7E)
val LedgerGreenPale = Color(0xFFD4F0E3)
val LedgerSurface = Color(0xFFF8F9FA)
val LedgerCard = Color(0xFFFFFFFF)
val LedgerSurfaceVariant = Color(0xFFF0F2F1)
val LedgerError = Color(0xFFD32F2F)
val LedgerOnPrimary = Color.White
val LedgerTextPrimary = Color(0xFF1A1C1B)
val LedgerTextSecondary = Color(0xFF6B7280)
val LedgerDivider = Color(0xFFE5E7EB)
val LedgerSuccess = Color(0xFF16A34A)
val LedgerWarning = Color(0xFFF59E0B)
val LedgerManagerBadge = Color(0xFFE8B931)
